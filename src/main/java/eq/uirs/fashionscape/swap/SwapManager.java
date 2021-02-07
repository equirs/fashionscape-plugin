package eq.uirs.fashionscape.swap;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.data.IdleAnimationID;
import eq.uirs.fashionscape.data.ItemInteractions;
import eq.uirs.fashionscape.panel.PanelKitType;
import java.util.Arrays;
import java.util.Collections;
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
import lombok.extern.slf4j.Slf4j;
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

@Singleton
@Slf4j
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

	private final SavedSwaps savedSwaps = new SavedSwaps();
	private final SnapshotQueues snapshotQueues = new SnapshotQueues(this::restoreSnapshot);
	// player's kit ids, e.g., hairstyles, base clothing
	private final Map<KitType, Integer> savedKitIds = new HashMap<>();

	private Snapshot hoverSnapshot;

	public void startUp()
	{
		checkForKitIds();
		refreshItemSwaps();
	}

	public void shutDown()
	{
		hoverSnapshot = null;
		savedSwaps.clear();
		savedSwaps.removeListeners();
		savedKitIds.clear();
		snapshotQueues.clear();
		snapshotQueues.removeListeners();
	}

	public void addItemChangeListener(BiConsumer<KitType, Integer> listener)
	{
		savedSwaps.addSwapListener(listener);
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
		return savedSwaps.entrySet().stream()
			.map(e -> swapItem(e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeWith)
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

	public void checkForKitIds()
	{
		for (KitType kitType : KitType.values())
		{
			Integer kitId = kitIdFor(kitType);
			if (kitId != null)
			{
				if (kitId > 0)
				{
					savedKitIds.put(kitType, kitId);
				}
				else if (!savedKitIds.containsKey(kitType))
				{
					// fall back to pre-filling a slot with a default model (better than nothing)
					Player player = client.getLocalPlayer();
					if (player != null)
					{
						Integer defaultKitId = null;
						PlayerComposition playerComposition = player.getPlayerComposition();
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
	}

	public void importSwaps(Map<KitType, Integer> newSwaps)
	{
		clientThread.invokeLater(() -> {
			Map<KitType, Integer> unlockedSwaps = newSwaps.entrySet().stream()
				.filter(e -> !savedSwaps.isLocked(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			savedSwaps.replaceAll(unlockedSwaps);
			unlockedSwaps.entrySet().stream()
				.map(e -> swapItem(e.getKey(), e.getValue()))
				.filter(Objects::nonNull)
				.reduce(Snapshot::mergeWith)
				.ifPresent(snapshotQueues::appendToUndo);
		});
	}

	// this should only be called from the client thread
	public List<String> stringifySwaps()
	{
		return savedSwaps.entrySet().stream()
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
		return savedSwaps.getOrDefault(slot, null);
	}

	public void revertSlot(KitType slot)
	{
		clientThread.invokeLater(() -> {
			Snapshot s = doRevert(slot);
			snapshotQueues.appendToUndo(s);
		});
	}

	public void hoverOver(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			Snapshot snapshot = swapItem(itemId, false);
			if (hoverSnapshot == null)
			{
				hoverSnapshot = snapshot;
			}
			else if (snapshot != null)
			{
				hoverSnapshot = snapshot.mergeWith(hoverSnapshot);
			}
		});
	}

	public void hoverSelect(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			KitType slot = slotForItem(itemId);
			if (savedSwaps.isLocked(slot))
			{
				return;
			}
			Snapshot snapshot;
			if (Objects.equals(savedSwaps.get(slot), itemId))
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
					snapshot = hoverSnapshot.mergeWith(snapshot);
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
		savedSwaps.clear();
		Arrays.stream(KitType.values())
			.map(slot -> {
				if (savedSwaps.isLocked(slot))
				{
					return null;
				}
				Integer itemId = equippedItemIdFor(slot);
				if (itemId != null && itemId != 0)
				{
					// revert to the player's actual equipped item
					return swapItem(slot, itemId);
				}
				else
				{
					// revert to the kit model if we have it, otherwise erase this slot
					int kitId = savedKitIds.getOrDefault(slot, 0);
					return swapKit(slot, kitId);
				}
			})
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeWith)
			.ifPresent(snapshotQueues::appendToUndo);
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
		clientThread.invokeLater(this::performShuffle);
	}

	private void performShuffle()
	{
		Map<KitType, Boolean> slotsToRevert = Arrays.stream(PanelKitType.values())
			.map(PanelKitType::getKitType)
			.filter(Objects::nonNull)
			.collect(Collectors.toMap(slot -> slot, savedSwaps::isLocked));
		slotsToRevert.put(KitType.JAW, true);
		slotsToRevert.put(KitType.HAIR, true);
		slotsToRevert.put(KitType.ARMS, true);

		// pre-fill slots that will be skipped:
		// save the swapped item id if the user wants to keep it, else use -1 as a placeholder for now
		Map<KitType, Integer> newSwaps = Arrays.stream(KitType.values())
			.filter(slotsToRevert::get)
			.collect(Collectors.toMap(slot -> slot, slot -> savedSwaps.getOrDefault(slot, -1)));
		List<Integer> randomOrder = IntStream.range(0, client.getItemCount()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomOrder);
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
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
				newSwaps.put(slot, itemComposition.getId());
				if (newSwaps.size() >= KitType.values().length)
				{
					break;
				}
			}
		}
		// slots filled with -1 were placeholders that need to be removed
		List<KitType> removes = newSwaps.entrySet().stream()
			.filter(e -> e.getValue() < 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		removes.forEach(newSwaps::remove);
		savedSwaps.replaceAll(newSwaps);
		Snapshot s = refreshItemSwaps();
		snapshotQueues.appendToUndo(s);
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
		if (snapshot != null && hoverSnapshot != null)
		{
			snapshot = snapshot.mergeWith(hoverSnapshot);
		}
		if (persist)
		{
			savedSwaps.put(slot, itemId);
			// only 1 item was swapped, so if the snapshot contains other slots, they must have been removed
			if (snapshot != null)
			{
				snapshot.getChangedEquipmentIds().keySet().stream()
					.filter(s -> !slot.equals(s))
					.forEach(savedSwaps::remove);
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
		int equipmentId = itemId == 0 ? 0 : itemId + 512;
		return swap(slot, equipmentId);
	}

	@Nullable
	private Snapshot swapKit(KitType slot, Integer kitId)
	{
		if (kitId == null)
		{
			return null;
		}
		int equipmentId = kitId == 0 ? 0 : kitId + 256;
		return swap(slot, equipmentId);
	}

	/*
	 * This is where most of the checks happen to make the swap look as expected, e.g., hiding hair, changing stance
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
		Map<KitType, Integer> changes = new HashMap<>();
		if (equipmentId == 0)
		{
			// hide slot (check if we need to show kit types the item was hiding)
			if (slot == KitType.HEAD)
			{
				int equipId = equipmentIdInSlot(KitType.HEAD);
				if (equipId >= 512 && !ItemInteractions.HAIR_HELMS.contains(equipId - 512))
				{
					int hairKitId = savedKitIds.getOrDefault(KitType.HAIR, 0);
					int hairEquipId = hairKitId > 0 ? hairKitId + 256 : 0;
					int oldId = setEquipmentId(composition, KitType.HAIR, hairEquipId);
					if (oldId != hairEquipId)
					{
						changes.put(KitType.HAIR, oldId);
					}
				}
				if (equipId >= 512 && ItemInteractions.NO_JAW_HELMS.contains(equipId - 512))
				{
					int jawKitId = savedKitIds.getOrDefault(KitType.JAW, 0);
					int jawEquipId = jawKitId > 0 ? jawKitId + 256 : 0;
					int oldId = setEquipmentId(composition, KitType.JAW, jawEquipId);
					if (oldId != jawEquipId)
					{
						changes.put(KitType.JAW, oldId);
					}
				}
			}
			else if (slot == KitType.TORSO)
			{
				int equipId = equipmentIdInSlot(KitType.TORSO);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512))
				{
					int armsKitId = savedKitIds.getOrDefault(KitType.ARMS, 0);
					int armsEquipId = armsKitId > 0 ? armsKitId + 256 : 0;
					int oldId = setEquipmentId(composition, KitType.ARMS, armsEquipId);
					if (oldId != armsEquipId)
					{
						changes.put(KitType.ARMS, oldId);
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
				changes.put(slot, oldId);
			}
			return new Snapshot(changes, animationId);
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
				Integer armsKitId = savedKitIds.get(KitType.ARMS);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512) && armsKitId != null)
				{
					int armsEquipId = armsKitId + 256;
					int oldId = setEquipmentId(composition, KitType.ARMS, armsEquipId);
					if (oldId != armsEquipId)
					{
						changes.put(KitType.ARMS, oldId);
					}
				}
			}
			int oldId = setEquipmentId(composition, slot, equipmentId);
			if (oldId != equipmentId)
			{
				changes.put(slot, oldId);
			}
			return new Snapshot(changes, null);
		}
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
					changes.put(KitType.SHIELD, oldId);
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
						changes.put(KitType.WEAPON, oldId);
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
				if (kitId != null && kitId != 0)
				{
					int oldId = setEquipmentId(composition, KitType.HAIR, kitId + 256);
					if (oldId != kitId + 256)
					{
						changes.put(KitType.HAIR, oldId);
					}
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.HAIR, 0);
				if (oldId != 0)
				{
					changes.put(KitType.HAIR, oldId);
				}
			}
			// check if we need to show/hide jaw
			if (!ItemInteractions.NO_JAW_HELMS.contains(itemId))
			{
				Integer kitId = savedKitIds.get(KitType.JAW);
				if (kitId != null && kitId != 0)
				{
					int oldId = setEquipmentId(composition, KitType.JAW, kitId + 256);
					if (oldId != kitId + 256)
					{
						changes.put(KitType.JAW, oldId);
					}
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.JAW, 0);
				if (oldId != 0)
				{
					changes.put(KitType.JAW, oldId);
				}
			}
		}
		if (slot == KitType.TORSO)
		{
			// check if we need to show/hide arms
			if (ItemInteractions.ARMS_TORSOS.contains(itemId))
			{
				Integer kitId = savedKitIds.get(KitType.ARMS);
				if (kitId != null && kitId != 0)
				{
					int oldId = setEquipmentId(composition, KitType.ARMS, kitId + 256);
					if (oldId != kitId + 256)
					{
						changes.put(KitType.ARMS, oldId);
					}
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.ARMS, 0);
				if (oldId != 0)
				{
					changes.put(KitType.ARMS, oldId);
				}
			}
		}
		int oldId = setEquipmentId(composition, slot, equipmentId);
		if (oldId != equipmentId)
		{
			changes.put(slot, oldId);
		}
		return new Snapshot(changes, animationId);
	}

	// Sets equipment id for slot and returns equipment id of previously occupied item.
	private int setEquipmentId(@Nonnull PlayerComposition composition, @Nonnull KitType slot, int equipmentId)
	{
		int previousId = composition.getEquipmentIds()[slot.getIndex()];
		composition.getEquipmentIds()[slot.getIndex()] = equipmentId;
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
		Integer originalKitId = savedKitIds.get(slot);
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
		savedSwaps.remove(slot);
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
		Integer itemId = savedSwaps.getOrDefault(kitType, equippedItemIdFor(kitType));
		if (itemId != null && itemId != 0)
		{
			return itemId + 512;
		}
		Integer kitId = kitIdFor(kitType);
		if (kitId != null && kitId != 0)
		{
			return kitId + 256;
		}
		return 0;
	}

	// returns the item id of the actual item equipped in the given slot (swaps ignored)
	@Nullable
	private Integer equippedItemIdFor(KitType kitType)
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
		if (item != null && item.getId() != 0)
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

	// restores snapshot and returns a new snapshot with reverted changes (allows redo)
	@Nullable
	private Snapshot restoreSnapshot(Snapshot snapshot)
	{
		return snapshot.getChangedEquipmentIds().entrySet()
			.stream()
			.map(e -> {
				KitType slot = e.getKey();
				Integer equipId = e.getValue();
				Snapshot s = swap(slot, equipId);
				if (equipId >= 512)
				{
					savedSwaps.put(slot, equipId - 512);
				}
				else
				{
					savedSwaps.remove(slot);
				}
				return s;
			})
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeWith)
			.orElse(null);
	}

}
