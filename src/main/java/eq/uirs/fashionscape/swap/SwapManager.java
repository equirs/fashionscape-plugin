package eq.uirs.fashionscape.swap;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.IdleAnimationID;
import eq.uirs.fashionscape.data.ItemInteractions;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

/**
 * Singleton class that maintains the memory and logic of swapping items through the plugin
 */
@Singleton
public class SwapManager
{

	private static final int DEFAULT_MALE_KIT_HAIR = 0;
	private static final int DEFAULT_MALE_KIT_JAW = 10;
	private static final int DEFAULT_MALE_KIT_ARMS = 26;
	private static final int DEFAULT_FEMALE_KIT_HAIR = 50;
	private static final int DEFAULT_FEMALE_KIT_ARMS = 61;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ColorScorer colorScorer;

	private final SavedSwaps savedSwaps = new SavedSwaps();
	private final SnapshotQueues snapshotQueues = new SnapshotQueues(this::restoreSnapshot);
	// player's real kit ids, e.g., hairstyles, base clothing
	private final Map<KitType, Integer> savedKitIds = new HashMap<>();
	// player's real base colors
	private final Map<ColorType, Integer> savedColors = new HashMap<>();

	// player's composition includes two int arrays, and the one that's size 5 is colors
	// this will break if Jagex ever adds another color type or another int[5] to the class, but w/e
	private String obfuscatedColorsFieldName = null;

	private Snapshot hoverSnapshot;

	@Value
	private static class Candidate
	{
		public int itemId;
		public KitType slot;
	}

	public void startUp()
	{
		checkForBaseIds();
		refreshItemSwaps();
	}

	public void shutDown()
	{
		savedSwaps.removeListeners();
		snapshotQueues.removeListeners();
		clear();
	}

	public void clear()
	{
		hoverSnapshot = null;
		savedSwaps.clear();
		savedKitIds.clear();
		snapshotQueues.clear();
	}

	public void addItemChangeListener(BiConsumer<KitType, Integer> listener)
	{
		savedSwaps.addItemSwapListener(listener);
	}

	public void addKitSwapListener(BiConsumer<KitType, Integer> listener)
	{
		savedSwaps.addKitSwapListener(listener);
	}

	public void addColorSwapListener(BiConsumer<ColorType, Integer> listener)
	{
		savedSwaps.addColorSwapListener(listener);
	}

	public void addLockChangeListener(BiConsumer<KitType, Boolean> listener)
	{
		savedSwaps.addLockListener(listener);
	}

	public void addUndoQueueChangeListener(Consumer<Integer> listener)
	{
		snapshotQueues.addUndoQueueChangeListener(listener);
	}

	public void addRedoQueueChangeListener(Consumer<Integer> listener)
	{
		snapshotQueues.addRedoQueueChangeListener(listener);
	}

	public boolean isLocked(KitType slot)
	{
		return savedSwaps.isLocked(slot);
	}

	public boolean isLocked(Integer itemId)
	{
		KitType slot = slotForItem(itemId);
		return isLocked(slot);
	}

	public void toggleLocked(KitType slot)
	{
		savedSwaps.toggleLocked(slot);
	}

	@Nullable
	// this should only be called from the client thread
	public Snapshot refreshItemSwaps()
	{
		return savedSwaps.itemEntries().stream()
			.sorted(Comparator.comparingInt(e -> -e.getKey().getIndex()))
			.map(e -> swapItem(e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeOver)
			.orElse(null);
	}

	public void undoLastSwap()
	{
		clientThread.invokeLater(() -> {
			if (client.getLocalPlayer() != null)
			{
				snapshotQueues.undoLast();
			}
		});
	}

	public boolean canUndo()
	{
		return snapshotQueues.undoSize() > 0;
	}

	public void redoLastSwap()
	{
		clientThread.invokeLater(() -> {
			if (client.getLocalPlayer() != null)
			{
				snapshotQueues.redoLast();
			}
		});
	}

	public boolean canRedo()
	{
		return snapshotQueues.redoSize() > 0;
	}

	/**
	 * Checks for the player's actual kits and colors so that they can be reverted
	 */
	public void checkForBaseIds()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		PlayerComposition playerComposition = player.getPlayerComposition();
		if (playerComposition.isFemale())
		{
			savedKitIds.remove(KitType.JAW);
		}
		for (KitType kitType : KitType.values())
		{
			Integer kitId = kitIdFor(kitType);
			if (kitId != null)
			{
				if (kitId >= 0)
				{
					savedKitIds.put(kitType, kitId);
				}
				else if (!savedKitIds.containsKey(kitType))
				{
					// fall back to pre-filling a slot with a default model (better than nothing)
					Integer defaultKitId = null;
					if (playerComposition.isFemale())
					{
						switch (kitType)
						{
							case ARMS:
								defaultKitId = DEFAULT_FEMALE_KIT_ARMS;
								break;
							case HAIR:
								defaultKitId = DEFAULT_FEMALE_KIT_HAIR;
								break;
							default:
								break;
						}
					}
					else
					{
						switch (kitType)
						{
							case ARMS:
								defaultKitId = DEFAULT_MALE_KIT_ARMS;
								break;
							case HAIR:
								defaultKitId = DEFAULT_MALE_KIT_HAIR;
								break;
							case JAW:
								defaultKitId = DEFAULT_MALE_KIT_JAW;
								break;
							default:
								break;
						}
					}
					if (defaultKitId != null)
					{
						savedKitIds.put(kitType, defaultKitId);
					}
				}
			}
		}
	}

	public void importSwaps(Map<KitType, Integer> newSwaps)
	{
		clientThread.invokeLater(() -> {
			Map<KitType, Integer> unlockedSwaps = newSwaps.entrySet().stream()
				.filter(e -> !savedSwaps.isLocked(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			unlockedSwaps.entrySet().stream()
				.sorted(Comparator.comparingInt(e -> -e.getKey().getIndex()))
				.map(e -> swapItem(e.getKey(), e.getValue()))
				.filter(Objects::nonNull)
				.reduce(Snapshot::mergeOver)
				.ifPresent(snapshotQueues::appendToUndo);
			savedSwaps.replaceAllItems(unlockedSwaps);
		});
	}

	// this should only be called from the client thread
	public List<String> stringifySwaps()
	{
		return savedSwaps.itemEntries().stream()
			.map(e -> {
				KitType slot = e.getKey();
				Integer itemId = e.getValue();
				String itemName = itemManager.getItemComposition(itemId).getName();
				return slot.name() + ":" + itemId + " (" + itemName + ")";
			})
			.collect(Collectors.toList());
	}

	@Nullable
	public Integer swappedItemIdIn(KitType slot)
	{
		return savedSwaps.getItem(slot);
	}

	@Nullable
	public Integer swappedKitIdIn(KitType slot)
	{
		return savedSwaps.getKit(slot);
	}

	@Nullable
	public Integer swappedColorIdIn(ColorType type)
	{
		return savedSwaps.getColor(type);
	}

	public void revertSlot(KitType slot, boolean force)
	{
		if (force)
		{
			savedSwaps.removeLock(slot);
		}
		clientThread.invokeLater(() -> {
			Snapshot s = doRevert(slot);
			snapshotQueues.appendToUndo(s);
		});
	}

	public void hoverOverItem(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			Snapshot snapshot = swapItem(itemId, false);
			if (hoverSnapshot == null)
			{
				hoverSnapshot = snapshot;
			}
			else if (snapshot != null)
			{
				hoverSnapshot = snapshot.mergeOver(hoverSnapshot);
			}
		});
	}

	public void hoverSelectItem(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			KitType slot = slotForItem(itemId);
			if (savedSwaps.isLocked(slot))
			{
				return;
			}
			Snapshot snapshot;
			if (Objects.equals(savedSwaps.getItem(slot), itemId))
			{
				snapshot = doRevert(slot);
			}
			else
			{
				snapshot = swapItem(itemId, true);
			}
			if (snapshot != null)
			{
				if (hoverSnapshot != null)
				{
					snapshot = hoverSnapshot.mergeOver(snapshot);
				}
				snapshotQueues.appendToUndo(snapshot);
			}
			hoverSnapshot = null;
		});
	}

	public void hoverAway()
	{
		clientThread.invokeLater(() -> {
			if (hoverSnapshot != null)
			{
				restoreSnapshot(hoverSnapshot);
				hoverSnapshot = null;
			}
			refreshItemSwaps();
		});
	}

	public void revertSwaps(boolean force)
	{
		clientThread.invokeLater(() -> performRevertSwaps(force));
	}

	private void performRevertSwaps(boolean force)
	{
		if (force)
		{
			savedSwaps.removeLocks();
		}
		Arrays.stream(KitType.values())
			.sorted(Comparator.comparingInt(s -> -s.getIndex()))
			.map(slot -> {
				// TODO how should we revert kits and colors...
				if (savedSwaps.isLocked(slot))
				{
					return null;
				}
				Integer itemId = equippedItemIdFor(slot);
				if (itemId != null && itemId >= 0)
				{
					// revert to the player's actual equipped item
					return swapItem(slot, itemId);
				}
				else
				{
					// revert to the kit model if we have it, otherwise erase this slot
					int kitId = savedKitIds.getOrDefault(slot, -256);
					return swapKit(slot, kitId);
				}
			})
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeOver)
			.ifPresent(snapshotQueues::appendToUndo);
		savedSwaps.clear();
	}

	@Nullable
	// this should only be called from the client thread
	public Integer slotIdFor(ItemComposition itemComposition)
	{
		ItemEquipmentStats equipStats = equipmentStatsFor(itemComposition.getId());
		if (equipStats != null)
		{
			return equipStats.getSlot();
		}
		return null;
	}

	public void shuffle()
	{
		// TODO add shuffle for kits and colors?
		clientThread.invokeLater(this::performShuffle);
	}

	private void performShuffle()
	{
		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		int size = intelligence.getDepth();
		if (size > 1)
		{
			Map<KitType, Integer> lockedSwaps = Arrays.stream(KitType.values())
				.filter(s -> savedSwaps.isLocked(s) && savedSwaps.containsItem(s))
				.collect(Collectors.toMap(s -> s, savedSwaps::getItem));
			colorScorer.setPlayerInfo(lockedSwaps);
		}
		Map<KitType, Boolean> slotsToRevert = Arrays.stream(PanelEquipSlot.values())
			.map(PanelEquipSlot::getKitType)
			.filter(Objects::nonNull)
			.collect(Collectors.toMap(slot -> slot, savedSwaps::isLocked));
		slotsToRevert.put(KitType.JAW, true);
		slotsToRevert.put(KitType.HAIR, true);
		slotsToRevert.put(KitType.ARMS, true);

		// pre-fill slots that will be skipped:
		// save the swapped item id if the user wants to keep it, else use -1 as a placeholder for now
		Map<KitType, Integer> newSwaps = Arrays.stream(KitType.values())
			.filter(slotsToRevert::get)
			.collect(Collectors.toMap(slot -> slot, slot -> savedSwaps.getItemOrDefault(slot, -1)));
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
		List<Candidate> candidates = new ArrayList<>(size);
		List<Integer> randomOrder = IntStream.range(0, client.getItemCount()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomOrder);
		for (Integer i : randomOrder)
		{
			int canonical = itemManager.canonicalize(i);
			if (skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(canonical);
			KitType slot = slotForId(slotIdFor(itemComposition));
			if (slot != null && !newSwaps.containsKey(slot))
			{
				// Don't equip a 2h weapon if we already have a shield
				if (slot == KitType.WEAPON)
				{
					ItemEquipmentStats stats = equipmentStatsFor(itemComposition.getId());
					if (stats != null && stats.isTwoHanded() && newSwaps.get(KitType.SHIELD) != null)
					{
						continue;
					}
				}
				// Don't equip a shield if we already have a 2h weapon (mark shield as removed)
				if (slot == KitType.SHIELD)
				{
					Integer weaponItemId = newSwaps.get(KitType.WEAPON);
					if (weaponItemId != null)
					{
						ItemEquipmentStats stats = equipmentStatsFor(weaponItemId);
						if (stats != null && stats.isTwoHanded())
						{
							newSwaps.put(KitType.SHIELD, -1);
							if (newSwaps.size() >= KitType.values().length)
							{
								break;
							}
							else
							{
								continue;
							}
						}
					}
				}
				candidates.add(new Candidate(itemComposition.getId(), slot));
			}

			if (!candidates.isEmpty() && candidates.size() >= size)
			{
				Candidate best;
				if (size > 1)
				{
					best = candidates.stream()
						.max(Comparator.comparingDouble(c -> colorScorer.score(c.itemId, c.slot)))
						.get();
					colorScorer.addPlayerInfo(best.slot, best.itemId);
				}
				else
				{
					best = candidates.get(0);
				}
				newSwaps.put(best.slot, best.itemId);
				if (newSwaps.size() >= KitType.values().length)
				{
					break;
				}
				candidates.clear();
			}
		}
		// slots filled with -1 were placeholders that need to be removed
		List<KitType> removes = newSwaps.entrySet().stream()
			.filter(e -> e.getValue() < 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		removes.forEach(newSwaps::remove);
		newSwaps.entrySet().stream()
			.sorted(Comparator.comparingInt(e -> -e.getKey().getIndex()))
			.map(e -> swapItem(e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeOver)
			.ifPresent(snapshotQueues::appendToUndo);
		savedSwaps.replaceAllItems(newSwaps);
	}

	@Nullable
	// this should only be called from the client thread
	public Snapshot swapItem(Integer itemId, boolean persist)
	{
		if (itemId == null)
		{
			return null;
		}
		ItemStats stats = itemManager.getItemStats(itemId, false);
		if (stats == null || stats.getEquipment() == null)
		{
			return null;
		}
		KitType slot = KitType.values()[stats.getEquipment().getSlot()];
		int equipmentId = itemId + 512;
		Snapshot snapshot = swap(slot, equipmentId);
		if (snapshot != null)
		{
			if (hoverSnapshot != null)
			{
				snapshot = snapshot.mergeOver(hoverSnapshot);
			}
			if (persist)
			{
				savedSwaps.putItem(slot, itemId);
				// only 1 item was swapped, so if the snapshot contains other slots, they must have been removed
				snapshot.getSlotChanges().keySet().stream()
					.filter(s -> !slot.equals(s))
					.forEach(savedSwaps::removeItem);
			}
		}

		return snapshot;
	}

	@Nullable
	// this should only be called from the client thread
	public Snapshot swapKit(KitType slot, Integer kitId, boolean persist)
	{
		if (kitId == null)
		{
			return null;
		}
		int equipmentId = kitId + 256;
		Snapshot snapshot = swap(slot, equipmentId);
		if (snapshot != null)
		{
			if (hoverSnapshot != null)
			{
				snapshot = snapshot.mergeOver(hoverSnapshot);
			}
			if (persist)
			{
				savedSwaps.putKit(slot, kitId);
				// only 1 kit was swapped, so if the snapshot contains other slots, they must have been removed
				snapshot.getSlotChanges().keySet().stream()
					.filter(s -> !slot.equals(s))
					.forEach(savedSwaps::removeItem);
			}
		}

		return snapshot;
	}

	@Nullable
	public Snapshot swapColor(ColorType type, Integer colorId, boolean persist)
	{
		if (colorId == null)
		{
			return null;
		}
		Snapshot snapshot = swapColor(type, colorId);
		if (snapshot != null)
		{
			if (hoverSnapshot != null)
			{
				snapshot = snapshot.mergeOver(hoverSnapshot);
			}
			if (persist)
			{
				savedSwaps.putColor(type, colorId);
			}
		}

		return snapshot;
	}

	@Nullable
	private Snapshot swapItem(KitType slot, Integer itemId)
	{
		if (itemId == null)
		{
			return null;
		}
		int equipmentId = itemId < 0 ? 0 : itemId + 512;
		return swap(slot, equipmentId);
	}

	@Nullable
	private Snapshot swapKit(KitType slot, Integer kitId)
	{
		if (kitId == null)
		{
			return null;
		}
		int equipmentId = kitId < 0 ? 0 : kitId + 256;
		return swap(slot, equipmentId);
	}

	@Nullable
	private Snapshot swapColor(ColorType type, Integer colorId)
	{
		if (type == null || colorId == null)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}

		try
		{
			int oldColorId = setColorId(composition, type, colorId);
			Map<ColorType, Snapshot.Change> changes = new HashMap<>();
			changes.put(type, new Snapshot.Change(oldColorId, savedSwaps.containsColor(type)));
			return new Snapshot(new HashMap<>(), changes, null);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/*
	 * This is where most of the checks happen to make the swap look as expected, e.g., hiding hair, changing stance.
	 * If multiple slots are being swapped simultaneously, swap SHIELD before WEAPON.
	 *
	 * equipmentId follows `PlayerComposition::getEquipmentId`:
	 * 0 for nothing
	 * 256-511 for a base kit model (i.e., kitId + 256)
	 * >=512 for an item (i.e., itemId + 512)
	 */
	@Nullable
	private Snapshot swap(KitType slot, int equipmentId)
	{
		if (slot == null)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}

		Integer animationId = null;
		Map<KitType, Snapshot.Change> changes = new HashMap<>();
		if (equipmentId == 0)
		{
			// hide slot (check if we need to show kit types the item was hiding)
			if (slot == KitType.HEAD)
			{
				int equipId = equipmentIdInSlot(KitType.HEAD);
				if (equipId >= 512 && !ItemInteractions.HAIR_HELMS.contains(equipId - 512))
				{
					int hairEquipId = savedKitIds.getOrDefault(KitType.HAIR, -256) + 256;
					int oldId = setEquipmentId(composition, KitType.HAIR, hairEquipId);
					if (oldId != hairEquipId)
					{
						changes.put(KitType.HAIR, new Snapshot.Change(oldId, false));
					}
				}
				if (equipId >= 512 && ItemInteractions.NO_JAW_HELMS.contains(equipId - 512))
				{
					int jawEquipId = savedKitIds.getOrDefault(KitType.JAW, -256) + 256;
					int oldId = setEquipmentId(composition, KitType.JAW, jawEquipId);
					if (oldId != jawEquipId)
					{
						changes.put(KitType.JAW, new Snapshot.Change(oldId, false));
					}
				}
			}
			else if (slot == KitType.TORSO)
			{
				int equipId = equipmentIdInSlot(KitType.TORSO);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512))
				{
					int armsEquipId = savedKitIds.getOrDefault(KitType.ARMS, -256) + 256;
					int oldId = setEquipmentId(composition, KitType.ARMS, armsEquipId);
					if (oldId != armsEquipId)
					{
						changes.put(KitType.ARMS, new Snapshot.Change(oldId, false));
					}
				}
			}
			else if (slot == KitType.WEAPON)
			{
				int oldAnimId = setIdleAnimationId(player, IdleAnimationID.DEFAULT);
				if (oldAnimId != IdleAnimationID.DEFAULT)
				{
					animationId = oldAnimId;
				}
			}
			int oldId = setEquipmentId(composition, slot, 0);
			if (oldId != 0)
			{
				changes.put(slot, new Snapshot.Change(oldId, savedSwaps.containsItem(slot)));
			}
		}
		else if (equipmentId <= 512)
		{
			// swapping to kit id (check if any equipment prevents this)
			if (slot == KitType.HAIR)
			{
				int equipId = equipmentIdInSlot(KitType.HEAD);
				if (equipId >= 512 && !ItemInteractions.HAIR_HELMS.contains(equipId - 512))
				{
					return null;
				}
			}
			else if (slot == KitType.JAW)
			{
				int equipId = equipmentIdInSlot(KitType.HEAD);
				if (equipId >= 512 && ItemInteractions.NO_JAW_HELMS.contains(equipId - 512))
				{
					return null;
				}
			}
			else if (slot == KitType.ARMS)
			{
				int equipId = equipmentIdInSlot(KitType.TORSO);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512))
				{
					return null;
				}
			}
			// if reverting torso to kit model, see if arms should be reverted to kit as well
			else if (slot == KitType.TORSO)
			{
				int equipId = equipmentIdInSlot(slot);
				int armsEquipId = savedKitIds.getOrDefault(KitType.ARMS, -256) + 256;
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512) && armsEquipId != 0)
				{
					int oldId = setEquipmentId(composition, KitType.ARMS, armsEquipId);
					if (oldId != armsEquipId)
					{
						changes.put(KitType.ARMS, new Snapshot.Change(oldId, false));
					}
				}
			}
			int oldId = setEquipmentId(composition, slot, equipmentId);
			if (oldId != equipmentId)
			{
				changes.put(slot, new Snapshot.Change(oldId, savedSwaps.containsItem(slot)));
			}
		}
		else
		{
			// otherwise, we're swapping to an item
			int itemId = equipmentId - 512;
			if (slot == KitType.WEAPON)
			{
				// check if equipping a 2h weapon. if so, remove shield
				ItemEquipmentStats stats = equipmentStatsFor(itemId);
				if (stats != null && stats.isTwoHanded())
				{
					int oldId = setEquipmentId(composition, KitType.SHIELD, 0);
					if (oldId != 0)
					{
						changes.put(KitType.SHIELD, new Snapshot.Change(oldId, savedSwaps.containsItem(KitType.SHIELD)));
					}
				}
				// check if weapon changes idle animation
				int newAnimationId = ItemInteractions.WEAPON_TO_IDLE.getOrDefault(itemId, IdleAnimationID.DEFAULT);
				int oldAnimId = setIdleAnimationId(player, newAnimationId);
				if (oldAnimId != newAnimationId)
				{
					animationId = oldAnimId;
				}
			}
			// check if already holding a 2h weapon. if so, un-equip it
			if (slot == KitType.SHIELD)
			{
				int weaponEquipmentId = equipmentIdInSlot(KitType.WEAPON);
				if (weaponEquipmentId != 0)
				{
					ItemEquipmentStats stats = equipmentStatsFor(weaponEquipmentId - 512);
					if (stats != null && stats.isTwoHanded())
					{
						int oldId = setEquipmentId(composition, KitType.WEAPON, 0);
						if (oldId != 0)
						{
							changes.put(KitType.WEAPON, new Snapshot.Change(oldId, savedSwaps.containsItem(KitType.WEAPON)));
						}
						int oldAnimId = setIdleAnimationId(player, IdleAnimationID.DEFAULT);
						if (oldAnimId != IdleAnimationID.DEFAULT)
						{
							animationId = oldAnimId;
						}
					}
				}
			}
			if (slot == KitType.HEAD)
			{
				// check if we need to show/hide hair
				if (ItemInteractions.HAIR_HELMS.contains(itemId))
				{
					Integer kitId = savedKitIds.get(KitType.HAIR);
					if (kitId != null && kitId >= 0)
					{
						int oldId = setEquipmentId(composition, KitType.HAIR, kitId + 256);
						if (oldId != kitId + 256)
						{
							changes.put(KitType.HAIR, new Snapshot.Change(oldId, false));
						}
					}
				}
				else
				{
					int oldId = setEquipmentId(composition, KitType.HAIR, 0);
					if (oldId != 0)
					{
						changes.put(KitType.HAIR, new Snapshot.Change(oldId, false));
					}
				}
				// check if we need to show/hide jaw
				if (!ItemInteractions.NO_JAW_HELMS.contains(itemId))
				{
					Integer kitId = savedKitIds.get(KitType.JAW);
					if (kitId != null && kitId >= 0)
					{
						int oldId = setEquipmentId(composition, KitType.JAW, kitId + 256);
						if (oldId != kitId + 256)
						{
							changes.put(KitType.JAW, new Snapshot.Change(oldId, false));
						}
					}
				}
				else
				{
					int oldId = setEquipmentId(composition, KitType.JAW, 0);
					if (oldId != 0)
					{
						changes.put(KitType.JAW, new Snapshot.Change(oldId, false));
					}
				}
			}
			if (slot == KitType.TORSO)
			{
				// check if we need to show/hide arms
				if (ItemInteractions.ARMS_TORSOS.contains(itemId))
				{
					Integer kitId = savedKitIds.get(KitType.ARMS);
					if (kitId != null && kitId >= 0)
					{
						int oldId = setEquipmentId(composition, KitType.ARMS, kitId + 256);
						if (oldId != kitId + 256)
						{
							changes.put(KitType.ARMS, new Snapshot.Change(oldId, false));
						}
					}
				}
				else
				{
					int oldId = setEquipmentId(composition, KitType.ARMS, 0);
					if (oldId != 0)
					{
						changes.put(KitType.ARMS, new Snapshot.Change(oldId, false));
					}
				}
			}
			int oldId = setEquipmentId(composition, slot, equipmentId);
			if (oldId != equipmentId)
			{
				changes.put(slot, new Snapshot.Change(oldId, savedSwaps.containsItem(slot)));
			}
		}

		return new Snapshot(changes, new HashMap<>(), animationId);
	}

	// Sets equipment id for slot and returns equipment id of previously occupied item.
	private int setEquipmentId(@Nonnull PlayerComposition composition, @Nonnull KitType slot, int equipmentId)
	{
		int previousId = composition.getEquipmentIds()[slot.getIndex()];
		composition.getEquipmentIds()[slot.getIndex()] = equipmentId;
		composition.setHash();
		return previousId;
	}

	// Sets color id for slot and returns color id before replacement.
	private int setColorId(@Nonnull PlayerComposition composition, @Nonnull ColorType type, int colorId)
		throws NoSuchFieldException, IllegalAccessException, IndexOutOfBoundsException
	{
		int[] colors = getColors(composition);
		int previousId = colors[type.ordinal()];
		colors[type.ordinal()] = colorId;
		composition.setHash();
		return previousId;
	}

	// Sets idle animation id for current player and returns previous idle animation
	private int setIdleAnimationId(@Nonnull Player player, int animationId)
	{
		int previousId = player.getIdlePoseAnimation();
		player.setIdlePoseAnimation(animationId);
		return previousId;
	}

	private Snapshot doRevert(KitType slot)
	{
		Integer originalItemId = equippedItemIdFor(slot);
		Integer originalKitId = swappedKitIdIn(slot);
		if (originalKitId == null)
		{
			originalKitId = savedKitIds.get(slot);
		}
		Snapshot s;
		if (originalItemId != null)
		{
			s = swapItem(slot, originalItemId);
		}
		else if (originalKitId != null)
		{
			s = swapKit(slot, originalKitId);
		}
		else
		{
			s = swap(slot, 0);
		}
		savedSwaps.removeItem(slot);
		return s;
	}

	@Nullable
	private KitType slotForId(Integer slotId)
	{
		if (slotId == null)
		{
			return null;
		}
		return Arrays.stream(KitType.values())
			.filter(type -> type.getIndex() == slotId)
			.findFirst()
			.orElse(null);
	}

	@Nullable
	private KitType slotForItem(Integer itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId, false);
		if (stats == null || stats.getEquipment() == null)
		{
			return null;
		}
		return KitType.values()[stats.getEquipment().getSlot()];
	}

	@Nullable
	private ItemEquipmentStats equipmentStatsFor(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId, false);
		if (stats != null && stats.isEquipable())
		{
			return stats.getEquipment();
		}
		return null;
	}

	// returns the equipment id of whatever is being displayed in the given slot
	private int equipmentIdInSlot(KitType kitType)
	{
		Integer itemId = savedSwaps.getItemOrDefault(kitType, equippedItemIdFor(kitType));
		if (itemId != null && itemId >= 0)
		{
			return itemId + 512;
		}
		Integer kitId = kitIdFor(kitType);
		if (kitId != null && kitId >= 0)
		{
			return kitId + 256;
		}
		return 0;
	}

	// returns the item id of the actual item equipped in the given slot (swaps ignored)
	@Nullable
	public Integer equippedItemIdFor(KitType kitType)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.EQUIPMENT);
		if (inventory == null)
		{
			return null;
		}
		Item item = inventory.getItem(kitType.getIndex());
		if (item != null && item.getId() >= 0)
		{
			return item.getId();
		}
		return null;
	}

	@Nullable
	private Integer kitIdFor(KitType kitType)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}
		return composition.getKitId(kitType);
	}

	@Nullable
	private Integer colorIdFor(ColorType colorType)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}
		try
		{
			int[] colors = getColors(composition);
			return colors[colorType.ordinal()];
		}
		catch (Exception e)
		{
			return null;
		}
	}

	// restores snapshot and returns a new snapshot with reverted changes (allows redo)
	@Nullable
	private Snapshot restoreSnapshot(Snapshot snapshot)
	{
		return snapshot.getSlotChanges().entrySet()
			.stream()
			.sorted(Comparator.comparingInt(e -> -e.getKey().getIndex()))
			.map(e -> {
				KitType slot = e.getKey();
				Snapshot.Change change = e.getValue();
				int equipId = change.getId();
				Snapshot s = swap(slot, equipId);
				if (equipId >= 512 && change.isUnnatural())
				{
					savedSwaps.putItem(slot, equipId - 512);
				}
				else
				{
					savedSwaps.removeItem(slot);
				}
				return s;
			})
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeOver)
			.orElse(null);
	}

	private int[] getColors(PlayerComposition composition) throws NoSuchFieldException, IllegalAccessException
	{
		// unfortunately, RuneLite does not expose the player composition colors, so we find them reflectively
		if (obfuscatedColorsFieldName == null)
		{
			findColorsField(composition);
		}
		Field colorsField = composition.getClass().getDeclaredField(obfuscatedColorsFieldName);
		colorsField.setAccessible(true);
		return (int[]) colorsField.get(composition);
	}

	private void findColorsField(PlayerComposition composition)
	{
		for (Field declaredField : composition.getClass().getDeclaredFields())
		{
			if (declaredField.getType().equals(int[].class))
			{
				try
				{
					declaredField.setAccessible(true);
					int[] arr = (int[]) declaredField.get(composition);
					if (arr.length == 5)
					{
						obfuscatedColorsFieldName = declaredField.getName();
					}
				}
				catch (IllegalAccessException e)
				{
					// do nothing
				}
			}
		}
	}

}
