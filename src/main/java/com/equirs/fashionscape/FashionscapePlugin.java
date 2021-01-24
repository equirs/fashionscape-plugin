package com.equirs.fashionscape;

import com.equirs.fashionscape.chatbox.ChatboxEquipmentSearch;
import com.equirs.fashionscape.chatbox.ChatboxSprites;
import com.equirs.fashionscape.data.IdleAnimationID;
import com.equirs.fashionscape.data.ItemInteractions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

@PluginDescriptor(
	name = "Fashionscape",
	description = "Previews combinations of equipment by changing the player's local appearance"
)
public class FashionscapePlugin extends Plugin
{

	private static final String CONFIG_GROUP = "fashionscape";
	private static final String OPTION_SWAP = "Swap";
	private static final String OPTION_REVERT = "Revert";
	private static final int MAX_SNAPSHOTS = 10;
	private static final int DEFAULT_MALE_KIT_HAIR = 0;
	private static final int DEFAULT_MALE_KIT_JAW = 10;
	private static final int DEFAULT_MALE_KIT_ARMS = 26;
	private static final int DEFAULT_FEMALE_KIT_HAIR = 50;
	private static final int DEFAULT_FEMALE_KIT_ARMS = 61;
	private static final Pattern PROFILE_PATTERN = Pattern.compile("^(\\w+):(\\d+).*");
	// menu entry's widgetId to KitType
	private static final Map<Integer, KitType> SLOT_WIDGET_IDS = new ImmutableMap.Builder<Integer, KitType>()
		.put(25362446, KitType.HEAD)
		.put(25362447, KitType.CAPE)
		.put(25362448, KitType.AMULET)
		.put(25362449, KitType.WEAPON)
		.put(25362450, KitType.TORSO)
		.put(25362451, KitType.SHIELD)
		.put(25362452, KitType.LEGS)
		.put(25362453, KitType.HANDS)
		.put(25362454, KitType.BOOTS)
		.build();

	@Inject
	private Client client;

	@Inject
	private ChatboxEquipmentSearch searchProvider;

	@Inject
	private ItemManager itemManager;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SpriteManager spriteManager;

	// manually specified item ids to display on the local player
	private final Map<KitType, Integer> swappedItemIds = new HashMap<>();
	// player's kit ids, e.g., hairstyles, base clothing
	private final Map<KitType, Integer> savedKitIds = new HashMap<>();

	private final LinkedList<Snapshot> undoSnapshots = new LinkedList<>();
	private final LinkedList<Snapshot> redoSnapshots = new LinkedList<>();
	private Snapshot hoverSnapshot;

	private final KeyListener keyListener = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (config.shuffleKey().matches(e))
			{
				shuffleEquipment();
			}
			else if (config.revertKey().matches(e))
			{
				revertSwaps();
			}
			else if (config.undoKey().matches(e))
			{
				undoLastSwap();
			}
			else if (config.redoKey().matches(e))
			{
				redoLastSwap();
			}
			else if (config.importKey().matches(e))
			{
				importProfile();
			}
			else if (config.exportKey().matches(e))
			{
				exportProfile();
			}
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	};

	@RequiredArgsConstructor
	@ToString
	private static class Snapshot
	{
		private final Map<KitType, Integer> changedEquipmentIds;
		private final Integer changedIdleAnimationId;

		// This snapshot will take priority of the other snapshot in the event of a collision.
		Snapshot mergeWith(Snapshot other)
		{
			Map<KitType, Integer> merged = new HashMap<>();
			merged.putAll(other.changedEquipmentIds);
			merged.putAll(this.changedEquipmentIds);
			Integer idleId = this.changedIdleAnimationId;
			if (idleId == null)
			{
				idleId = other.changedIdleAnimationId;
			}
			return new Snapshot(merged, idleId);
		}
	}

	@Provides
	FashionscapeConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FashionscapeConfig.class);
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(keyListener);
		spriteManager.addSpriteOverrides(ChatboxSprites.values());
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(keyListener);
		revertSwaps();
		hoverSnapshot = null;
		swappedItemIds.clear();
		savedKitIds.clear();
		undoSnapshots.clear();
		redoSnapshots.clear();
		spriteManager.removeSpriteOverrides(ChatboxSprites.values());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		Integer idleId = config.idleAnimId();
		Player player = client.getLocalPlayer();
		if (player != null && player.getIdlePoseAnimation() != idleId)
		{
			player.setIdlePoseAnimation(idleId);
		}
	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged event)
	{
		Player player = event.getPlayer();
		if (player != null && player == client.getLocalPlayer())
		{
			checkForKitIds();
			refreshItemSwaps();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry entry = Arrays.stream(event.getMenuEntries())
			.filter(menuEntry -> menuEntry.getParam1() != 0)
			.findFirst()
			.orElse(null);
		if (entry == null || SLOT_WIDGET_IDS.get(entry.getParam1()) == null)
		{
			return;
		}
		KitType slot = SLOT_WIDGET_IDS.get(entry.getParam1());
		MenuEntry swapEntry = new MenuEntry();
		swapEntry.setOption(OPTION_SWAP);
		swapEntry.setTarget(entry.getTarget());
		swapEntry.setType(MenuAction.RUNELITE.getId());
		swapEntry.setParam0(entry.getParam0());
		swapEntry.setParam1(entry.getParam1());
		swapEntry.setIdentifier(entry.getIdentifier());

		MenuEntry[] newMenu = ObjectArrays.concat(client.getMenuEntries(), swapEntry);
		if (swappedItemIds.containsKey(slot))
		{
			MenuEntry revertEntry = new MenuEntry();
			revertEntry.setOption(OPTION_REVERT);
			revertEntry.setTarget(entry.getTarget());
			revertEntry.setType(MenuAction.RUNELITE.getId());
			revertEntry.setParam0(entry.getParam0());
			revertEntry.setParam1(entry.getParam1());
			revertEntry.setIdentifier(entry.getIdentifier());
			MenuEntry[] menuPlusRevert = ObjectArrays.concat(newMenu, revertEntry);
			client.setMenuEntries(menuPlusRevert);
		}
		else
		{
			client.setMenuEntries(newMenu);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equals(OPTION_SWAP) && SLOT_WIDGET_IDS.containsKey(event.getWidgetId()))
		{
			searchProvider
				.tooltipText("Try on")
				.filterDuplicateIcons(false)
				.allowEmpty(true)
				.maxResults(Integer.MAX_VALUE)
				.filter(itemComposition -> {
					Integer itemSlotId = slotIdFor(itemComposition);
					KitType slot = SLOT_WIDGET_IDS.get(event.getWidgetId());
					// TODO check non-standard items
					return itemSlotId != null && itemSlotId.equals(slot.getIndex());
				})
				.onItemHovered(id -> {
					KitType slot = SLOT_WIDGET_IDS.get(event.getWidgetId());
					if (slot != null)
					{
						Snapshot snapshot = swapItem(slot, id);
						if (hoverSnapshot == null)
						{
							hoverSnapshot = snapshot;
						}
						else if (snapshot != null)
						{
							hoverSnapshot = snapshot.mergeWith(hoverSnapshot);
						}
					}
				})
				.onItemSelected(id -> {
					KitType slot = SLOT_WIDGET_IDS.get(event.getWidgetId());
					if (slot != null)
					{
						swappedItemIds.put(slot, id);
						swapItem(slot, id);
						hoverSnapshot = null;
					}
				})
				.onClose(() -> {
					if (hoverSnapshot != null)
					{
						restoreSnapshot(hoverSnapshot);
						hoverSnapshot = null;
					}
					refreshItemSwaps();
				})
				.build();
		}
		else if (event.getMenuOption().equals(OPTION_REVERT) && SLOT_WIDGET_IDS.containsKey(event.getWidgetId()))
		{
			KitType slot = SLOT_WIDGET_IDS.get(event.getWidgetId());
			Integer originalItemId = equippedItemIdFor(slot);
			if (originalItemId != null)
			{
				swappedItemIds.remove(slot);
				swapItem(slot, originalItemId);
			}
		}
	}

	private void shuffleEquipment()
	{
		Map<KitType, Boolean> slotsToRevert = new ImmutableMap.Builder<KitType, Boolean>()
			.put(KitType.AMULET, !config.shuffleAmulet())
			.put(KitType.BOOTS, !config.shuffleBoots())
			.put(KitType.CAPE, !config.shuffleCape())
			.put(KitType.HANDS, !config.shuffleHands())
			.put(KitType.HEAD, !config.shuffleHead())
			.put(KitType.LEGS, !config.shuffleLegs())
			.put(KitType.SHIELD, !config.shuffleShield())
			.put(KitType.TORSO, !config.shuffleTorso())
			.put(KitType.WEAPON, !config.shuffleWeapon())
			.put(KitType.JAW, true)
			.put(KitType.HAIR, true)
			.put(KitType.ARMS, true)
			.build();
		// save the swapped item id if the user wants to keep it, else use -1 as a placeholder for now
		Map<KitType, Integer> revertSwaps = Arrays.stream(KitType.values())
			.filter(slotsToRevert::get)
			.collect(Collectors.toMap(slot -> slot, slot -> swappedItemIds.getOrDefault(slot, -1)));
		swappedItemIds.clear();
		// pre-fill slots that will be skipped
		swappedItemIds.putAll(revertSwaps);
		List<Integer> randomOrder = IntStream.range(0, client.getItemCount()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomOrder);
		for (Integer i : randomOrder)
		{
			ItemComposition itemComposition = itemManager.getItemComposition(itemManager.canonicalize(i));
			// TODO check non-standard items
			KitType slot = slotFor(slotIdFor(itemComposition));
			if (slot != null && !swappedItemIds.containsKey(slot))
			{
				// Don't equip a 2h weapon if we already have a shield
				if (slot == KitType.WEAPON)
				{
					ItemEquipmentStats stats = equipmentStatsFor(itemComposition.getId());
					if (stats != null && stats.isTwoHanded() && swappedItemIds.get(KitType.SHIELD) != null)
					{
						continue;
					}
				}
				// Don't equip a shield if we already have a 2h weapon (mark shield as removed)
				if (slot == KitType.SHIELD)
				{
					Integer weaponItemId = swappedItemIds.get(KitType.WEAPON);
					if (weaponItemId != null)
					{
						ItemEquipmentStats stats = equipmentStatsFor(weaponItemId);
						if (stats != null && stats.isTwoHanded())
						{
							swappedItemIds.put(KitType.SHIELD, -1);
							if (swappedItemIds.size() >= KitType.values().length)
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
				swappedItemIds.put(slot, itemComposition.getId());
				if (swappedItemIds.size() >= KitType.values().length)
				{
					break;
				}
			}
		}
		// slots filled with -1 were placeholders that need to be removed
		List<KitType> removes = swappedItemIds.entrySet().stream()
			.filter(e -> e.getValue() < 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		removes.forEach(swappedItemIds::remove);
		Snapshot shuffle = refreshItemSwaps();
		addUndoSnapshot(shuffle);
		redoSnapshots.clear();
	}

	@Nullable
	private Snapshot refreshItemSwaps()
	{
		return swappedItemIds.entrySet().stream()
			.map(e -> swapItem(e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeWith)
			.orElse(null);
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
					int oldId = setEquipmentId(composition, KitType.HAIR, hairKitId);
					changes.put(KitType.HAIR, oldId);
				}
				if (equipId >= 512 && ItemInteractions.NO_JAW_HELMS.contains(equipId - 512))
				{
					int jawKitId = savedKitIds.getOrDefault(KitType.JAW, 0);
					int oldId = setEquipmentId(composition, KitType.JAW, jawKitId);
					changes.put(KitType.JAW, oldId);
				}
			}
			else if (slot == KitType.TORSO)
			{
				int equipId = equipmentIdInSlot(KitType.TORSO);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512))
				{
					int armsKitId = savedKitIds.getOrDefault(KitType.ARMS, 0);
					int oldId = setEquipmentId(composition, KitType.ARMS, armsKitId);
					changes.put(KitType.ARMS, oldId);
				}
			}
			else if (slot == KitType.WEAPON)
			{
				animationId = setIdleAnimationId(player, IdleAnimationID.DEFAULT);
			}
			int oldId = setEquipmentId(composition, slot, 0);
			changes.put(slot, oldId);
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
			// check if removing an item which normally obscures hair/jaw/arms, and if so, put those back
			else if (slot == KitType.HEAD)
			{
				int equipId = equipmentIdInSlot(slot);
				Integer hairKitId = savedKitIds.get(KitType.HAIR);
				Integer jawKitId = savedKitIds.get(KitType.JAW);
				if (equipId >= 512 && !ItemInteractions.HAIR_HELMS.contains(equipId - 512) && hairKitId != null)
				{
					int oldId = setEquipmentId(composition, KitType.HAIR, hairKitId - 256);
					changes.put(KitType.HAIR, oldId);
				}
				if (equipId >= 512 && ItemInteractions.NO_JAW_HELMS.contains(equipId - 512) && jawKitId != null)
				{
					int oldId = setEquipmentId(composition, KitType.JAW, jawKitId - 256);
					changes.put(KitType.JAW, oldId);
				}
			}
			else if (slot == KitType.TORSO)
			{
				int equipId = equipmentIdInSlot(slot);
				Integer armsKitId = savedKitIds.get(KitType.ARMS);
				if (equipId >= 512 && !ItemInteractions.ARMS_TORSOS.contains(equipId - 512) && armsKitId != null)
				{
					int oldId = setEquipmentId(composition, KitType.ARMS, armsKitId - 256);
					changes.put(KitType.ARMS, oldId);
				}
			}
			int oldId = setEquipmentId(composition, slot, equipmentId);
			changes.put(slot, oldId);
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
				changes.put(KitType.SHIELD, oldId);
			}
			// check if weapon changes idle animation
			int newAnimationId = ItemInteractions.WEAPON_TO_IDLE.getOrDefault(itemId, IdleAnimationID.DEFAULT);
			animationId = setIdleAnimationId(player, newAnimationId);
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
					changes.put(KitType.WEAPON, oldId);
					animationId = setIdleAnimationId(player, IdleAnimationID.DEFAULT);
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
					changes.put(KitType.HAIR, oldId);
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.HAIR, 0);
				changes.put(KitType.HAIR, oldId);
			}
			// check if we need to show/hide jaw
			if (!ItemInteractions.NO_JAW_HELMS.contains(itemId))
			{
				Integer kitId = savedKitIds.get(KitType.JAW);
				if (kitId != null && kitId != 0)
				{
					int oldId = setEquipmentId(composition, KitType.JAW, kitId + 256);
					changes.put(KitType.JAW, oldId);
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.JAW, 0);
				changes.put(KitType.JAW, oldId);
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
					changes.put(KitType.ARMS, oldId);
				}
			}
			else
			{
				int oldId = setEquipmentId(composition, KitType.ARMS, 0);
				changes.put(KitType.ARMS, oldId);
			}
		}
		int oldId = setEquipmentId(composition, slot, equipmentId);
		changes.put(slot, oldId);
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

	private void revertSwaps()
	{
		swappedItemIds.clear();
		Arrays.stream(KitType.values())
			.map(slot -> {
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
			.ifPresent(snapshot -> {
				addUndoSnapshot(snapshot);
				redoSnapshots.clear();
			});
	}

	@Nullable
	private Integer slotIdFor(ItemComposition itemComposition)
	{
		ItemEquipmentStats equipStats = equipmentStatsFor(itemComposition.getId());
		if (equipStats != null)
		{
			return equipStats.getSlot();
		}
		return null;
	}

	@Nullable
	private KitType slotFor(Integer slotId)
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
		Integer itemId = swappedItemIds.getOrDefault(kitType, equippedItemIdFor(kitType));
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

	// returns the item id of the actual item equipped in the given slot
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

	private void checkForKitIds()
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
		return snapshot.changedEquipmentIds.entrySet()
			.stream().map(e -> swap(e.getKey(), e.getValue()))
			.filter(Objects::nonNull)
			.reduce(Snapshot::mergeWith)
			.orElse(null);
	}

	private void addUndoSnapshot(@Nullable Snapshot snapshot)
	{
		if (snapshot == null)
		{
			return;
		}
		Snapshot lastUndo = undoSnapshots.peekLast();
		if (lastUndo != null && snapshot.changedEquipmentIds.equals(lastUndo.changedEquipmentIds))
		{
			return;
		}
		undoSnapshots.add(snapshot);
		while (undoSnapshots.size() > MAX_SNAPSHOTS)
		{
			undoSnapshots.removeFirst();
		}
	}

	private void addRedoSnapshot(@Nullable Snapshot snapshot)
	{
		if (snapshot == null)
		{
			return;
		}
		Snapshot lastRedo = redoSnapshots.peekLast();
		if (lastRedo != null && snapshot.changedEquipmentIds.equals(lastRedo.changedEquipmentIds))
		{
			return;
		}
		redoSnapshots.add(snapshot);
		while (redoSnapshots.size() > MAX_SNAPSHOTS)
		{
			redoSnapshots.removeFirst();
		}
	}

	private void undoLastSwap()
	{
		if (undoSnapshots.isEmpty() || client.getLocalPlayer() == null)
		{
			return;
		}
		Snapshot last = undoSnapshots.removeLast();
		Snapshot redo = restoreSnapshot(last);
		addRedoSnapshot(redo);
	}

	private void redoLastSwap()
	{
		if (redoSnapshots.isEmpty() || client.getLocalPlayer() == null)
		{
			return;
		}
		Snapshot last = redoSnapshots.removeLast();
		Snapshot restore = restoreSnapshot(last);
		addUndoSnapshot(restore);
	}

	private void importProfile()
	{
		String[] allLines = config.savedProfile().split("\n");
		for (String line : allLines)
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			Matcher matcher = PROFILE_PATTERN.matcher(line);
			if (matcher.matches())
			{
				String slotStr = matcher.group(1).toLowerCase();
				int itemId = Integer.parseInt(matcher.group(2));
				KitType slot = stringMatch(slotStr);
				if (slot != null)
				{
					swappedItemIds.put(slot, itemId);
					swapItem(slot, itemId);
				}
				else
				{
					sendHighlightedMessage("Could not process line: " + line);
				}
			}
		}
	}

	private void exportProfile()
	{
		List<String> lines = new ArrayList<>();
		for (KitType slot : KitType.values())
		{
			Integer itemId = swappedItemIds.get(slot);
			if (itemId != null)
			{
				String itemName = itemManager.getItemComposition(itemId).getName();
				lines.add(slot.name().toLowerCase() + ":" + itemId + " (" + itemName + ")");
			}
		}
		String joined = String.join("\n", lines);
		if (joined.isEmpty())
		{
			sendHighlightedMessage("No outfit was found. Only swapped items will be saved.");
		}
		else
		{
			configManager.setConfiguration(CONFIG_GROUP, "savedProfile", joined);
			sendHighlightedMessage("Outfit saved to Fashionscape plugin config.");
		}
	}

	private void sendHighlightedMessage(String message)
	{
		String chatMessage = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(message)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(chatMessage)
			.build());
	}

	@Nullable
	private KitType stringMatch(String name)
	{
		switch (name)
		{
			case "helm":
			case "helmet":
			case "head":
			case "hat":
				return KitType.HEAD;
			case "cape":
			case "back":
				return KitType.CAPE;
			case "neck":
			case "amulet":
			case "necklace":
				return KitType.AMULET;
			case "weapon":
				return KitType.WEAPON;
			case "torso":
			case "body":
			case "chest":
			case "top":
			case "shirt":
				return KitType.TORSO;
			case "shield":
			case "offhand":
				return KitType.SHIELD;
			case "leg":
			case "legs":
			case "bottom":
			case "bottoms":
			case "pants":
				return KitType.LEGS;
			case "hand":
			case "hands":
			case "glove":
			case "gloves":
				return KitType.HANDS;
			case "boot":
			case "boots":
			case "feet":
			case "shoes":
				return KitType.BOOTS;
			default:
				return null;
		}
	}

}
