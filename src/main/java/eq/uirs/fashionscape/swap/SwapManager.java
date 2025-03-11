package eq.uirs.fashionscape.swap;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.data.BootsColor;
import eq.uirs.fashionscape.data.ClothingColor;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.Colorable;
import eq.uirs.fashionscape.data.HairColor;
import eq.uirs.fashionscape.data.IdleAnimationID;
import eq.uirs.fashionscape.data.ItemInteractions;
import eq.uirs.fashionscape.data.SkinColor;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import eq.uirs.fashionscape.swap.event.SwapEvent;
import eq.uirs.fashionscape.swap.event.SwapEventListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Singleton class that maintains the memory and logic of swapping items through the plugin
 */
@Singleton
@Slf4j
public class SwapManager
{
	public static final Map<Integer, Kit> KIT_ID_TO_KIT = new HashMap<>();
	// item-only slots that can always contain an equipment id of 0
	public static final List<KitType> ALLOWS_NOTHING = ImmutableList.of(KitType.HEAD, KitType.CAPE, KitType.AMULET,
		KitType.WEAPON, KitType.SHIELD);
	public static final int ITEM_OFFSET = PlayerComposition.ITEM_OFFSET;
	public static final int KIT_OFFSET = PlayerComposition.KIT_OFFSET;

	private static final Map<KitType, List<Kit>> KIT_TYPE_TO_KITS;
	private static final List<KitType> NEVER_ZERO_SLOTS = ImmutableList.of(KitType.TORSO, KitType.LEGS,
		KitType.HAIR, KitType.HANDS, KitType.BOOTS, KitType.JAW);
	private static final String KIT_SUFFIX = "_KIT";
	private static final String COLOR_SUFFIX = "_COLOR";
	private static final String ICON_KEY = "ICON";

	static
	{
		KIT_TYPE_TO_KITS = Arrays.stream(KitType.values())
			.collect(Collectors.toMap(s -> s, s -> Arrays.asList(Kit.allInSlot(s, true))));
		for (KitType slot : KitType.values())
		{
			for (Kit value : Kit.allInSlot(slot, true))
			{
				Integer mascId = value.getKitId(0);
				if (mascId != null)
				{
					KIT_ID_TO_KIT.put(mascId, value);
				}
				Integer femId = value.getKitId(1);
				if (femId != null)
				{
					KIT_ID_TO_KIT.put(femId, value);
				}
			}
		}
	}

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

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private SavedSwaps savedSwaps;

	private final SwapDiffHistory swapDiffHistory = new SwapDiffHistory(s -> this.restore(s, true));

	@Setter
	private Integer gender;
	private String lastKnownPlayerName = null;
	private SwapDiff hoverSwapDiff;
	// slot -> override equipment id, used to disable the plugin's functionality per slot
	private final Map<KitType, Integer> disabledSlots = new HashMap<>();
	// idle anim id to switch to when weapon slot is disabled (sometimes sourced from non-weapons like minecart)
	private Integer disabledAnimationId = null;

	@Value
	private static class Candidate
	{
		public int itemId;
		public KitType slot;
	}

	public void startUp()
	{
		doPreRefreshCheck();
		refreshAllSwaps();
	}

	public void shutDown()
	{
		revertSwaps(true, true);
		savedSwaps.removeListeners();
		swapDiffHistory.removeListeners();
		hoverSwapDiff = null;
	}

	public void onPlayerChanged()
	{
		doPreRefreshCheck();
		savedSwaps.loadFromConfig();
		refreshAllSwaps();
	}

	public void addEventListener(SwapEventListener<? extends SwapEvent> listener)
	{
		savedSwaps.addEventListener(listener);
	}

	public void addUndoQueueChangeListener(Consumer<Integer> listener)
	{
		swapDiffHistory.addUndoQueueChangeListener(listener);
	}

	public void addRedoQueueChangeListener(Consumer<Integer> listener)
	{
		swapDiffHistory.addRedoQueueChangeListener(listener);
	}

	public boolean isSlotLocked(KitType slot)
	{
		return savedSwaps.isSlotLocked(slot);
	}

	public boolean isKitLocked(KitType slot)
	{
		return savedSwaps.isKitLocked(slot);
	}

	public boolean isItemLocked(KitType slot)
	{
		return savedSwaps.isItemLocked(slot);
	}

	public boolean isColorLocked(ColorType type)
	{
		return savedSwaps.isColorLocked(type);
	}

	public boolean isIconLocked()
	{
		return savedSwaps.isIconLocked();
	}

	public void toggleItemLocked(KitType slot)
	{
		savedSwaps.toggleItemLocked(slot);
	}

	public void toggleKitLocked(KitType slot)
	{
		savedSwaps.toggleKitLocked(slot);
	}

	public void toggleColorLocked(ColorType type)
	{
		savedSwaps.toggleColorLocked(type);
	}

	public void toggleIconLocked()
	{
		savedSwaps.toggleIconLocked();
	}

	// this should only be called from the client thread
	public void refreshAllSwaps()
	{
		Map<KitType, Integer> savedEquipmentIds = savedSwaps.itemEntries().stream().collect(
			Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + ITEM_OFFSET)
		);
		Map<KitType, Integer> savedKitEquipIds = savedSwaps.kitEntries().stream().collect(
			Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + KIT_OFFSET)
		);
		Map<KitType, Integer> hiddenEquipIds = savedSwaps.getHiddenSlots().stream().collect(
			Collectors.toMap(v -> v, v -> 0)
		);
		savedEquipmentIds.putAll(savedKitEquipIds);
		savedEquipmentIds.putAll(hiddenEquipIds);
		for (CompoundSwap c : CompoundSwap.fromMap(sanitize(savedEquipmentIds), savedSwaps.getSwappedIcon()))
		{
			swap(c, SwapMode.PREVIEW);
		}
		for (Map.Entry<ColorType, Integer> e : savedSwaps.colorEntries())
		{
			swap(e.getKey(), e.getValue(), SwapMode.PREVIEW);
		}
	}

	/**
	 * Undoes the last action performed.
	 * Can only be called from the client thread.
	 */
	public void undoLastSwap()
	{
		if (client.getLocalPlayer() != null)
		{
			swapDiffHistory.undoLast();
		}
	}

	public boolean canUndo()
	{
		return swapDiffHistory.undoSize() > 0;
	}

	/**
	 * Redoes the last action that was undone.
	 * Can only be called from the client thread.
	 */
	public void redoLastSwap()
	{
		if (client.getLocalPlayer() != null)
		{
			swapDiffHistory.redoLast();
		}
	}

	public boolean canRedo()
	{
		return swapDiffHistory.redoSize() > 0;
	}

	/**
	 * Check for:
	 * - player's gender, match against last known (non-null) gender. if no match, revert and clear all kit swaps
	 * - player's real kit ids and real colors so that they can be reverted
	 * - player's real icon in the jaw slot (playing Barbarian Assault or Soul Wars)
	 * - any kit/item ids that should disable swaps (e.g., minecart, magic carpet)
	 */
	public void doPreRefreshCheck()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		PlayerComposition playerComposition = player.getPlayerComposition();
		if (playerComposition == null)
		{
			return;
		}
		int gender = playerComposition.getGender();
		checkRealKits(gender);
		this.gender = gender;
		if (!Objects.equals(lastKnownPlayerName, player.getName()))
		{
			savedSwaps.loadRSProfileConfig();
			lastKnownPlayerName = player.getName();
		}
		for (KitType kitType : KitType.values())
		{
			Integer kitId = kitIdFor(kitType);
			if (kitId != null)
			{
				if (kitId >= 0 && (!savedSwaps.containsSlot(kitType) ||
					!Objects.equals(savedSwaps.getKit(kitType), kitId)))
				{
					savedSwaps.putRealKit(kitType, kitId);
				}
			}
		}
		for (ColorType colorType : ColorType.values())
		{
			Integer colorId = colorIdFor(colorType);
			if (colorId != null && (!savedSwaps.containsColor(colorType) ||
				!Objects.equals(savedSwaps.getColor(colorType), colorId)))
			{
				savedSwaps.putRealColor(colorType, colorId);
			}
		}
		int jawEquipId = playerComposition.getEquipmentIds()[KitType.JAW.getIndex()];
		if (jawEquipId >= ITEM_OFFSET)
		{
			JawIcon icon = JawKit.iconFromItemId(jawEquipId - ITEM_OFFSET);
			savedSwaps.putRealIcon(icon);
		}
		else
		{
			savedSwaps.putRealIcon(JawIcon.NOTHING);
		}
		int bootKitId = playerComposition.getKitId(KitType.BOOTS);
		int weaponItemId = playerComposition.getEquipmentIds()[KitType.WEAPON.getIndex()] - ITEM_OFFSET;
		if (ItemInteractions.DISABLE_BOOT_KITS.contains(bootKitId))
		{
			disabledAnimationId = IdleAnimationID.MINECART;
			disabledSlots.put(KitType.BOOTS, bootKitId + KIT_OFFSET);
			disabledSlots.put(KitType.WEAPON, 0);
			disabledSlots.put(KitType.SHIELD, 0);
		}
		else if (ItemInteractions.DISABLE_WEAPONS.contains(weaponItemId))
		{
			disabledSlots.put(KitType.WEAPON, weaponItemId + ITEM_OFFSET);
			disabledSlots.put(KitType.SHIELD, 0);
		}
		else
		{
			disabledAnimationId = null;
			disabledSlots.clear();
		}
		refreshDisabledSlots();
	}

	private void checkRealKits(Integer gender)
	{
		boolean containsOppositeGenderKits = savedSwaps.getRealKitIds().entrySet().stream()
			.anyMatch(e -> {
				Kit kit = KIT_ID_TO_KIT.get(e.getValue());
				return kit != null && kit.getKitId(gender) == null;
			});
		if (containsOppositeGenderKits)
		{
			savedSwaps.clearRealKits();
		}
	}

	private void refreshDisabledSlots()
	{
		if (!disabledSlots.isEmpty())
		{
			// temporarily remove locks while swapping disabled slots
			Set<KitType> lockedItems = savedSwaps.getAllLockedItems();
			Set<KitType> lockedKits = savedSwaps.getAllLockedKits();
			boolean iconLocked = savedSwaps.isIconLocked();
			for (KitType slot : disabledSlots.keySet())
			{
				savedSwaps.removeSlotLock(slot);
				savedSwaps.removeIconLock();
			}
			if (!disabledSlots.isEmpty())
			{
				CompoundSwap.fromMap(disabledSlots, null)
					.forEach(c -> swap(c, true, SwapMode.PREVIEW));
			}
			if (disabledAnimationId != null)
			{
				Player player = client.getLocalPlayer();
				if (player != null)
				{
					setIdleAnimationId(player, disabledAnimationId, true);
				}
				disabledAnimationId = null;
			}
			savedSwaps.setLockedItems(lockedItems);
			savedSwaps.setLockedKits(lockedKits);
			savedSwaps.setIconLocked(iconLocked);
		}
	}

	/**
	 * Sanity checks after changing equipment:
	 * <p>
	 * If there's an actual item in a slot and no virtual swap in that slot, BUT other kit swaps hide
	 * the item, do a one-off unsaved swap to hide this newly-equipped item.
	 */
	public void onEquipmentChanged()
	{
		for (KitType kitType : KitType.values())
		{
			Integer inventoryItemId = inventoryItemId(kitType);
			Integer virtualItemId = savedSwaps.getItem(kitType);
			Integer virtualKitId = savedSwaps.getKit(kitType);
			if (virtualItemId == null && virtualKitId == null && inventoryItemId != null)
			{
				switch (kitType)
				{
					case HEAD:
						if ((savedSwaps.containsSlot(KitType.HAIR) &&
							!ItemInteractions.HAIR_HELMS.contains(inventoryItemId)) ||
							(savedSwaps.containsSlot(KitType.JAW) &&
								ItemInteractions.NO_JAW_HELMS.contains(inventoryItemId)))
						{
							swap(CompoundSwap.single(KitType.HEAD, 0), SwapMode.PREVIEW);
						}
						else if (savedSwaps.containsIcon())
						{
							// if icon is swapped but not hair/jaw, just refresh (head is not in conflict)
							swap(CompoundSwap.single(KitType.HEAD, inventoryItemId + ITEM_OFFSET), SwapMode.PREVIEW);
						}
						break;
					case TORSO:
						if (savedSwaps.containsSlot(KitType.ARMS) &&
							!ItemInteractions.ARMS_TORSOS.contains(inventoryItemId))
						{
							int kitId = savedSwaps.getRealKit(kitType, gender);
							swap(CompoundSwap.single(kitType, kitId + KIT_OFFSET), SwapMode.PREVIEW);
						}
						break;
					default:
						break;
				}
			}
		}
	}

	public void importSwaps(
		Map<KitType, Integer> newItems,
		Map<KitType, Integer> newKits,
		Map<ColorType, Integer> newColors,
		JawIcon icon,
		Set<KitType> slotsToRemove)
	{
		clientThread.invokeLater(() -> {
			// prepare swaps for import
			Map<KitType, Integer> itemEquipSwaps = newItems.entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + ITEM_OFFSET)
			);
			Map<KitType, Integer> kitEquipSwaps = newKits.entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + KIT_OFFSET)
			);
			Map<KitType, Integer> removals = slotsToRemove.stream().collect(
				Collectors.toMap(v -> v, v -> 0)
			);
			Map<KitType, Integer> equipSwaps = new HashMap<>(kitEquipSwaps);
			equipSwaps.putAll(itemEquipSwaps);
			equipSwaps.putAll(removals);

			// remove locks and revert everything on player
			savedSwaps.removeAllLocks();
			SwapDiff iconRevert = doRevertIcon();
			SwapDiff kitRevert = Arrays.stream(KitType.values())
				.map(this::doRevert)
				.filter(Objects::nonNull)
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());
			SwapDiff colorRevert = Arrays.stream(ColorType.values())
				.map(this::doRevert)
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());

			SwapDiff equips = CompoundSwap.fromMap(sanitize(equipSwaps), icon).stream()
				.map(c -> this.swap(c, SwapMode.SAVE))
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());

			Map<ColorType, Integer> colorSwaps = newColors.entrySet().stream()
				.filter(e -> !savedSwaps.isColorLocked(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			Map<ColorType, Integer> colorEquipSwaps = colorSwaps.entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
			);

			SwapDiff colors = colorEquipSwaps.entrySet().stream()
				.map(e -> this.swapColor(e.getKey(), e.getValue(), true))
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());
			SwapDiff total = iconRevert
				.mergeOver(kitRevert)
				.mergeOver(colorRevert)
				.mergeOver(equips)
				.mergeOver(colors);
			swapDiffHistory.appendToUndo(total);
		});
	}

	// this should only be called from the client thread
	public List<String> stringifySwaps()
	{
		Set<Map.Entry<KitType, Integer>> itemEntries = new HashSet<>(savedSwaps.hiddenSlotEntries());
		itemEntries.addAll(savedSwaps.itemEntries());
		List<String> items = itemEntries.stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				int itemId = e.getValue();
				if (itemId == 0)
				{
					return slot.name() + ":-1 (Nothing)";
				}
				else
				{
					String itemName = itemManager.getItemComposition(itemId).getMembersName();
					return slot.name() + ":" + itemId + " (" + itemName + ")";
				}
			})
			.collect(Collectors.toList());
		List<String> kits = savedSwaps.kitEntries().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				Integer kitId = e.getValue();
				String kitName = KIT_ID_TO_KIT.get(kitId).getDisplayName();
				return slot.name() + KIT_SUFFIX + ":" + kitId + " (" + kitName + ")";
			})
			.collect(Collectors.toList());
		List<String> colors = savedSwaps.colorEntries().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				ColorType type = e.getKey();
				int colorId = e.getValue();
				return Arrays.stream(type.getColorables())
					.filter(c -> c.getColorId(type) == colorId)
					.findFirst()
					.map(c -> type.name() + COLOR_SUFFIX + ":" + colorId + " (" + c.getDisplayName() + ")")
					.orElse("");
			})
			.collect(Collectors.toList());
		List<String> icons = new ArrayList<>();
		JawIcon icon = savedSwaps.getSwappedIcon();
		if (savedSwaps.containsIcon())
		{
			icons.add(ICON_KEY + ":" + icon.getId() + " (" + icon.getDisplayName() + ")");
		}
		items.addAll(kits);
		items.addAll(colors);
		items.addAll(icons);
		return items;
	}

	@Nullable
	public Integer swappedItemIdIn(KitType slot)
	{
		return savedSwaps.getItem(slot);
	}

	@Nullable
	public JawIcon swappedIcon()
	{
		return savedSwaps.getSwappedIcon();
	}

	public boolean isHidden(KitType slot)
	{
		return savedSwaps.isHidden(slot);
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

	public Map<ColorType, Colorable> swappedColorsMap()
	{
		BiFunction<ColorType, Integer, Colorable> getColor = (type, id) -> {
			switch (type)
			{
				case HAIR:
					return HairColor.fromId(id);
				case TORSO:
					return ClothingColor.fromTorsoId(id);
				case LEGS:
					return ClothingColor.fromLegsId(id);
				case BOOTS:
					return BootsColor.fromId(id);
				case SKIN:
					return SkinColor.fromId(id);
			}
			return null;
		};
		return Arrays.stream(ColorType.values())
			.filter(savedSwaps::containsColor)
			.collect(Collectors.toMap(t -> t, t -> getColor.apply(t, savedSwaps.getColor(t))));
	}

	// this should only be called from the client thread
	public void revert(KitType slot, ColorType type)
	{
		SwapDiff s = SwapDiff.blank();
		if (slot != null)
		{
			savedSwaps.removeSlotLock(slot);
			s = s.mergeOver(doRevert(slot));
		}
		if (type != null)
		{
			savedSwaps.removeColorLock(type);
			s = s.mergeOver(doRevert(type));
		}
		swapDiffHistory.appendToUndo(s);
	}

	// this should only be called from the client thread
	public void revertSlot(KitType slot)
	{
		savedSwaps.removeSlotLock(slot);
		SwapDiff s = doRevert(slot);
		swapDiffHistory.appendToUndo(s);
	}

	public void revertIcon()
	{
		savedSwaps.removeIconLock();
		SwapDiff s = doRevertIcon();
		swapDiffHistory.appendToUndo(s);
	}

	public void hoverOverItem(KitType slot, Integer itemId)
	{
		hoverOver(() -> {
			int id = itemId < 0 ? -ITEM_OFFSET : itemId;
			return swapItem(slot, id, false, false);
		});
	}

	public void hoverOverKit(KitType slot, int kitId)
	{
		hoverOver(() -> swapKit(slot, kitId, false));
	}

	public void hoverOverColor(ColorType type, Integer colorId)
	{
		hoverOver(() -> swapColor(type, colorId, false));
	}

	public void hoverOverIcon(JawIcon icon)
	{
		hoverOver(() -> swapIcon(icon, false));
	}

	private void hoverOver(Supplier<SwapDiff> diffCallable)
	{
		clientThread.invokeLater(() -> {
			SwapDiff swapDiff = diffCallable.get();
			if (hoverSwapDiff == null)
			{
				hoverSwapDiff = swapDiff;
			}
			else if (!swapDiff.isBlank())
			{
				hoverSwapDiff = swapDiff.mergeOver(hoverSwapDiff);
			}
		});
	}

	public void hoverSelectItem(KitType slot, Integer itemId)
	{
		hoverSelect(() -> {
			if (savedSwaps.isItemLocked(slot))
			{
				return SwapDiff.blank();
			}
			if (itemId < 0)
			{
				return savedSwaps.isHidden(slot) ?
					doRevert(slot) :
					swapItem(slot, -ITEM_OFFSET, true, false);
			}
			else
			{
				return Objects.equals(savedSwaps.getItem(slot), itemId) ?
					doRevert(slot) :
					swapItem(slot, itemId, true, false);
			}
		});
	}

	public void hoverSelectKit(KitType slot, int kitId)
	{
		hoverSelect(() -> {
			if (savedSwaps.isKitLocked(slot))
			{
				return SwapDiff.blank();
			}
			return Objects.equals(savedSwaps.getKit(slot), kitId) ?
				doRevert(slot) :
				swapKit(slot, kitId, true);
		});
	}

	public void hoverSelectColor(ColorType type, Integer colorId)
	{
		hoverSelect(() -> {
			if (savedSwaps.isColorLocked(type))
			{
				return SwapDiff.blank();
			}
			return Objects.equals(savedSwaps.getColor(type), colorId) ?
				doRevert(type) :
				swapColor(type, colorId, true);
		});
	}

	public void hoverSelectIcon(JawIcon icon)
	{
		hoverSelect(() -> {
			if (savedSwaps.isIconLocked())
			{
				return SwapDiff.blank();
			}
			return savedSwaps.getSwappedIcon() == icon ?
				doRevertIcon() :
				swapIcon(icon, true);
		});
	}

	private void hoverSelect(Supplier<SwapDiff> diffSupplier)
	{
		clientThread.invokeLater(() -> {
			SwapDiff swapDiff = diffSupplier.get();
			if (!swapDiff.isBlank())
			{
				if (hoverSwapDiff != null)
				{
					swapDiff = hoverSwapDiff.mergeOver(swapDiff);
				}
				swapDiffHistory.appendToUndo(swapDiff);
				hoverSwapDiff = null;
			}
		});
	}

	public void hoverAway()
	{
		clientThread.invokeLater(() -> {
			if (hoverSwapDiff != null)
			{
				restore(hoverSwapDiff, false);
				hoverSwapDiff = null;
			}
			refreshAllSwaps();
		});
	}

	// reverts only the given swaps. if removeLocks is true, only removes locks for those slots.
	private void revertSwaps(List<KitType> slots, boolean removeLocks, boolean preview)
	{
		if (removeLocks)
		{
			slots.forEach(slot -> savedSwaps.removeSlotLock(slot));
			savedSwaps.removeIconLock();
		}
		SwapMode mode = preview ? SwapMode.PREVIEW : SwapMode.REVERT;
		JawIcon icon = null;
		if (!savedSwaps.isIconLocked())
		{
			icon = JawIcon.NOTHING;
		}
		Map<KitType, Integer> equipIdsToRevert = slots.stream()
			.filter(slot -> !savedSwaps.isSlotLocked(slot))
			.collect(Collectors.toMap(slot -> slot, slot -> {
				Integer itemId = inventoryItemId(slot);
				if (itemId != null && itemId != 0)
				{
					return itemId + ITEM_OFFSET;
				}
				return savedSwaps.getRealKit(slot, gender) + KIT_OFFSET;
			}));
		SwapDiff kitsDiff = CompoundSwap.fromMap(equipIdsToRevert, icon).stream()
			.map(c -> this.swap(c, false, (s) -> mode, mode))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		SwapDiff colorsDiff = Arrays.stream(ColorType.values())
			.filter(type -> !savedSwaps.isColorLocked(type))
			.map(type -> {
				Integer colorId = savedSwaps.getRealColor(type);
				return colorId != null ? swap(type, colorId, mode) : SwapDiff.blank();
			})
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		SwapDiff totalDiff = kitsDiff
			.mergeOver(colorsDiff);
		if (mode == SwapMode.REVERT)
		{
			swapDiffHistory.appendToUndo(totalDiff);
			savedSwaps.clearSwapped();
		}
	}

	/**
	 * Reverts all item/kit slots, colors, and icon. Unless `removeLocks` is true, locked slots will remain.
	 * If `preview` is true, saved swaps will be unaffected.
	 * Can only be called from the client thread.
	 */
	public void revertSwaps(boolean removeLocks, boolean preview)
	{
		revertSwaps(Arrays.asList(KitType.values()), removeLocks, preview);
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

	public void loadImports(List<String> allLines)
	{
		Map<KitType, Integer> itemImports = new HashMap<>();
		Map<KitType, Integer> kitImports = new HashMap<>();
		Map<ColorType, Integer> colorImports = new HashMap<>();
		JawIcon icon = null;
		Set<KitType> removes = new HashSet<>();
		for (String line : allLines)
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			Matcher matcher = FashionscapePlugin.PROFILE_PATTERN.matcher(line);
			if (matcher.matches())
			{
				String slotStr = matcher.group(1);
				// could be item id, kit id, or color id
				int id = Integer.parseInt(matcher.group(2));
				KitType itemSlot = itemSlotMatch(slotStr);
				KitType kitSlot = kitSlotMatch(slotStr);
				ColorType colorType = colorSlotMatch(slotStr);
				if (itemSlot != null)
				{
					if (id <= 0)
					{
						removes.add(itemSlot);
					}
					else
					{
						itemImports.put(itemSlot, id);
					}
				}
				else if (kitSlot != null)
				{
					kitImports.put(kitSlot, id);
				}
				else if (colorType != null)
				{
					colorImports.put(colorType, id);
				}
				else if (slotStr.equals(ICON_KEY))
				{
					icon = JawIcon.fromId(id);
				}
				else
				{
					sendHighlightedMessage("Could not import line: " + line);
				}
			}
		}
		// if not explicitly included, items without data should be explicitly hidden
		ALLOWS_NOTHING.forEach(slot -> {
			if (!itemImports.containsKey(slot))
			{
				removes.add(slot);
			}
		});
		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty())
		{
			importSwaps(itemImports, kitImports, colorImports, icon, removes);
		}
	}

	public void exportSwaps(File selected)
	{
		clientThread.invokeLater(() -> {
			try (PrintWriter out = new PrintWriter(selected))
			{
				List<String> exports = stringifySwaps();
				for (String line : exports)
				{
					out.println(line);
				}
				sendHighlightedMessage("Saved fashionscape to " + selected.getName());
			}
			catch (FileNotFoundException e)
			{
				log.warn("Could not find selected file for swaps export", e);
			}
		});
	}

	public void copyOutfit(PlayerComposition other)
	{
		int[] equipmentIds = other.getEquipmentIds();
		KitType[] slots = KitType.values();
		Map<KitType, Integer> equipIdImports = IntStream.range(0, equipmentIds.length).boxed()
			.collect(Collectors.toMap(i -> slots[i], i -> equipmentIds[i]));

		Map<KitType, Integer> itemImports = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() >= ITEM_OFFSET)
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - ITEM_OFFSET));
		Map<KitType, Integer> kitImports = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() >= KIT_OFFSET && e.getValue() < ITEM_OFFSET)
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - KIT_OFFSET));
		Set<KitType> removals = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() <= 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		int[] colors = other.getColors();
		ColorType[] types = ColorType.values();
		Map<ColorType, Integer> colorImports = IntStream.range(0, colors.length).boxed()
			.collect(Collectors.toMap(i -> types[i], i -> colors[i]));

		JawIcon icon = null;
		Integer jawItemId = itemImports.get(KitType.JAW);
		if (jawItemId != null)
		{
			icon = JawKit.iconFromItemId(jawItemId);
			itemImports.remove(KitType.JAW);
		}

		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty() || !removals.isEmpty())
		{
			importSwaps(itemImports, kitImports, colorImports, icon, removals);
		}
	}

	/**
	 * Takes a map of equipment ids and returns a new map, replacing any equipment ids that are inapplicable to
	 * the current player's gender with equipment ids that are (if no analogous ids are found, those slots are removed)
	 */
	private Map<KitType, Integer> sanitize(Map<KitType, Integer> equipmentIds)
	{
		Integer gender = this.gender;
		if (gender == null)
		{
			return new HashMap<>();
		}
		Map<KitType, Integer> newEquipIds = new HashMap<>();
		equipmentIds.forEach((slot, equipId) -> {
			if (equipId >= KIT_OFFSET && equipId < ITEM_OFFSET || equipId >= ITEM_OFFSET && slot == KitType.JAW)
			{
				Integer newEquipId = getAnalogousEquipmentId(equipId);
				if (newEquipId != null)
				{
					newEquipIds.put(slot, newEquipId);
				}
			}
			else if (equipId >= ITEM_OFFSET)
			{
				newEquipIds.put(slot, equipId);
			}
		});
		Map<KitType, Integer> removals = equipmentIds.entrySet().stream()
			.filter(e -> e.getValue() == 0 && !NEVER_ZERO_SLOTS.contains(e.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> 0));
		newEquipIds.putAll(removals);
		return newEquipIds;
	}

	/**
	 * Attempts to match the given (kit/icon) equipment id with one that matches
	 * the current player's gender. Returns null if no analogue is found.
	 */
	@Nullable
	private Integer getAnalogousEquipmentId(int equipId)
	{
		Integer result = null;
		JawIcon icon = JawIcon.NOTHING;
		if (equipId >= ITEM_OFFSET)
		{
			JawKit kit = JawKit.fromEquipmentId(equipId);
			if (kit != null)
			{
				icon = JawKit.iconFromItemId(equipId - ITEM_OFFSET);
				Integer jawKitId = kit.getKitId(gender);
				if (jawKitId != null)
				{
					equipId = jawKitId + KIT_OFFSET;
				}
			}
		}
		Kit kit = KIT_ID_TO_KIT.get(equipId - KIT_OFFSET);
		if (kit != null)
		{
			Integer kitId = kit.getKitId(gender);
			if (kitId != null)
			{
				result = kitId + KIT_OFFSET;
			}
			else
			{
				BiMap<Kit, Kit> lookup = Objects.equals(gender, 0) ?
					ItemInteractions.GENDER_MIRRORED_KITS :
					ItemInteractions.GENDER_MIRRORED_KITS.inverse();
				if (lookup.containsKey(kit))
				{
					Integer analogKitId = lookup.get(kit).getKitId(gender);
					if (analogKitId != null)
					{
						result = analogKitId + KIT_OFFSET;
					}
				}
			}
			if (icon != JawIcon.NOTHING)
			{
				if (result != null)
				{
					Integer itemId = JawKit.fromEquipmentId(result).getIconItemId(icon);
					result = itemId != null ? itemId + ITEM_OFFSET : result;
				}
				else
				{
					result = combineJawIcon(null, icon);
				}
			}
		}
		return result;
	}

	/**
	 * Randomizes items/kits/colors in unlocked slots.
	 * Can only be called from the client thread.
	 */
	public void shuffle()
	{
		final Random r = new Random();
		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		int size = intelligence.getDepth();
		if (size > 1)
		{
			Map<KitType, Integer> lockedItems = Arrays.stream(KitType.values())
				.filter(s -> savedSwaps.isItemLocked(s) && savedSwaps.containsItem(s))
				.collect(Collectors.toMap(s -> s, savedSwaps::getItem));
			if (savedSwaps.isIconLocked() && savedSwaps.containsIcon())
			{
				Integer iconItemId = JawKit.NO_JAW.getIconItemId(savedSwaps.getSwappedIcon());
				if (iconItemId != null)
				{
					lockedItems.put(KitType.JAW, iconItemId);
				}
			}
			Map<ColorType, Colorable> lockedColors = swappedColorsMap().entrySet().stream()
				.filter(e -> savedSwaps.isColorLocked(e.getKey()) && savedSwaps.containsColor(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			colorScorer.setPlayerInfo(lockedItems, lockedColors);
		}
		Map<KitType, Boolean> itemSlotsToRevert = Arrays.stream(KitType.values())
			.collect(Collectors.toMap(slot -> slot, savedSwaps::isItemLocked));

		// pre-fill slots that will be skipped with -1 as a placeholder
		Map<KitType, Integer> newSwaps = Arrays.stream(KitType.values())
			.filter(itemSlotsToRevert::get)
			.collect(Collectors.toMap(s -> s, s -> -1));
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
		List<Candidate> candidates = new ArrayList<>(size);
		List<Integer> randomOrder = IntStream.range(0, client.getItemCount()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomOrder);
		Iterator<Integer> randomIterator = randomOrder.iterator();
		while (newSwaps.size() < KitType.values().length && randomIterator.hasNext())
		{
			Integer i = randomIterator.next();
			int canonical = itemManager.canonicalize(i);
			if (skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(canonical);
			int itemId = itemComposition.getId();
			KitType slot = slotForId(slotIdFor(itemComposition));
			if (slot != null && !newSwaps.containsKey(slot))
			{
				// Don't equip a 2h weapon if we already have a shield
				if (slot == KitType.WEAPON)
				{
					ItemEquipmentStats stats = equipmentStatsFor(itemId);
					if (stats != null && stats.isTwoHanded() && newSwaps.get(KitType.SHIELD) != null)
					{
						continue;
					}
				}
				// Don't equip a shield if we already have a 2h weapon (mark shield as removed instead)
				else if (slot == KitType.SHIELD)
				{
					Integer weaponItemId = newSwaps.get(KitType.WEAPON);
					if (weaponItemId != null)
					{
						ItemEquipmentStats stats = equipmentStatsFor(weaponItemId);
						if (stats != null && stats.isTwoHanded())
						{
							newSwaps.put(KitType.SHIELD, -1);
							continue;
						}
					}
				}
				else if (slot == KitType.HEAD)
				{
					// Don't equip a helm if it hides hair and hair is locked
					if (!ItemInteractions.HAIR_HELMS.contains(itemId) && savedSwaps.isKitLocked(KitType.HAIR))
					{
						continue;
					}
					// Don't equip a helm if it hides jaw and jaw is locked
					if (ItemInteractions.NO_JAW_HELMS.contains(itemId) && savedSwaps.isKitLocked(KitType.JAW))
					{
						continue;
					}
				}
				else if (slot == KitType.TORSO)
				{
					// Don't equip torso if it hides arms and arms is locked
					if (!ItemInteractions.ARMS_TORSOS.contains(itemId) && savedSwaps.isKitLocked(KitType.ARMS))
					{
						continue;
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
				candidates.clear();
			}
		}
		// slots filled with -1 were placeholders that need to be removed
		List<KitType> removes = newSwaps.entrySet().stream()
			.filter(e -> e.getValue() < 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		removes.forEach(newSwaps::remove);

		// shuffle colors
		Map<ColorType, Integer> newColors = new HashMap<>();
		List<ColorType> allColorTypes = Arrays.asList(ColorType.values().clone());
		Collections.shuffle(allColorTypes);
		for (ColorType type : allColorTypes)
		{
			if (savedSwaps.isColorLocked(type))
			{
				continue;
			}
			List<Colorable> colorables = Arrays.asList(type.getColorables().clone());
			if (colorables.isEmpty())
			{
				continue;
			}
			Collections.shuffle(colorables);
			int limit;
			switch (intelligence)
			{
				case LOW:
					limit = Math.max(1, colorables.size() / 4);
					break;
				case MODERATE:
					limit = Math.max(1, colorables.size() / 2);
					break;
				case HIGH:
					limit = colorables.size();
					break;
				default:
					limit = 1;
			}
			Colorable best = colorables.stream()
				.limit(limit)
				.max(Comparator.comparingDouble(c -> colorScorer.score(c, type)))
				.orElse(colorables.get(0));
			colorScorer.addPlayerColor(type, best);
			newColors.put(type, best.getColorId(type));
		}

		SwapDiff totalDiff = SwapDiff.blank();

		// convert to equipment ids
		Map<KitType, Integer> newEquipSwaps = newSwaps.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + ITEM_OFFSET));

		// swap items now before moving on to kits
		SwapDiff itemsDiff = CompoundSwap.fromMap(newEquipSwaps, null).stream()
			.map(c -> this.swap(c, SwapMode.SAVE))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		totalDiff = totalDiff.mergeOver(itemsDiff);

		// See if remaining slots can be kit-swapped
		if (gender != null && !config.excludeBaseModels())
		{
			Map<KitType, Integer> kitSwaps = Arrays.stream(KitType.values())
				.filter(slot -> !newSwaps.containsKey(slot) && isOpen(slot))
				.map(slot -> {
					List<Kit> kits = KIT_TYPE_TO_KITS.getOrDefault(slot, new ArrayList<>()).stream()
						.filter(k -> k.getKitId(gender) != null)
						.collect(Collectors.toList());
					return kits.isEmpty() ? null : kits.get(r.nextInt(kits.size()));
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Kit::getKitType, k -> k.getKitId(gender)));

			Map<KitType, Integer> kitEquipSwaps = kitSwaps.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + KIT_OFFSET));

			SwapDiff kitsDiff = CompoundSwap.fromMap(kitEquipSwaps, null)
				.stream()
				.map(c -> this.swap(c, SwapMode.SAVE))
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());

			totalDiff = totalDiff.mergeOver(kitsDiff);
		}

		if (!config.excludeNonStandardItems() && !config.excludeMembersItems() && !savedSwaps.isIconLocked())
		{
			List<JawIcon> icons = Arrays.asList(JawIcon.values());
			Collections.shuffle(icons);
			int limit = icons.size();
			if (intelligence == RandomizerIntelligence.NONE)
			{
				limit = 1;
			}
			Map<JawIcon, Double> scores = icons.stream()
				.limit(limit)
				.collect(Collectors.toMap(i -> i, i -> {
					Integer itemId = JawKit.NO_JAW.getIconItemId(i);
					return itemId != null ? colorScorer.score(itemId, null) : 0;
				}));
			// only icon swap if >75% match (if intelligence is > NONE)
			JawIcon icon = scores.entrySet().stream()
				.filter(e -> intelligence == RandomizerIntelligence.NONE || e.getValue() > 0.75)
				.max(Comparator.comparingDouble(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(JawIcon.NOTHING);
			SwapDiff iconDiff = swap(CompoundSwap.fromIcon(icon), SwapMode.PREVIEW, SwapMode.SAVE);
			totalDiff = totalDiff.mergeOver(iconDiff);
		}

		SwapDiff colorsDiff = newColors.entrySet().stream()
			.filter(e -> e.getValue() >= 0)
			.map(e -> swap(e.getKey(), e.getValue(), SwapMode.SAVE))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());

		totalDiff = totalDiff.mergeOver(colorsDiff);

		swapDiffHistory.appendToUndo(totalDiff);
	}

	// this should only be called from the client thread
	public SwapDiff swapItem(KitType slot, Integer itemId, boolean save, boolean saveIcon)
	{
		if (itemId == null)
		{
			return SwapDiff.blank();
		}
		int equipmentId = itemId + ITEM_OFFSET;
		SwapMode swapMode = save ? SwapMode.SAVE : SwapMode.PREVIEW;
		SwapMode iconSwapMode = saveIcon ? SwapMode.SAVE : SwapMode.PREVIEW;
		SwapDiff swapDiff = swap(CompoundSwap.single(slot, equipmentId), swapMode, iconSwapMode);
		if (hoverSwapDiff != null)
		{
			swapDiff = swapDiff.mergeOver(hoverSwapDiff);
		}
		return swapDiff;
	}

	// this should only be called from the client thread
	public SwapDiff swapKit(KitType slot, Integer kitId, boolean save)
	{
		if (kitId == null)
		{
			return SwapDiff.blank();
		}
		int equipmentId = kitId + KIT_OFFSET;
		SwapMode swapMode = save ? SwapMode.SAVE : SwapMode.PREVIEW;
		SwapDiff swapDiff = swap(CompoundSwap.single(slot, equipmentId), swapMode, SwapMode.PREVIEW);
		if (hoverSwapDiff != null)
		{
			swapDiff = swapDiff.mergeOver(hoverSwapDiff);
		}
		return swapDiff;
	}

	// this should only be called from the client thread
	public SwapDiff swapIcon(JawIcon icon, boolean save)
	{
		if (icon == null)
		{
			return SwapDiff.blank();
		}
		SwapMode swapMode = save ? SwapMode.SAVE : SwapMode.PREVIEW;
		SwapDiff swapDiff = swap(CompoundSwap.fromIcon(icon), SwapMode.PREVIEW, swapMode);
		if (hoverSwapDiff != null)
		{
			swapDiff = swapDiff.mergeOver(hoverSwapDiff);
		}
		return swapDiff;
	}

	// this should only be called from the client thread
	public SwapDiff swapColor(ColorType type, Integer colorId, boolean save)
	{
		if (colorId == null)
		{
			return SwapDiff.blank();
		}
		SwapMode swapMode = save ? SwapMode.SAVE : SwapMode.PREVIEW;
		SwapDiff swapDiff = swap(type, colorId, swapMode);
		if (hoverSwapDiff != null)
		{
			swapDiff = swapDiff.mergeOver(hoverSwapDiff);
		}
		return swapDiff;
	}

	/**
	 * directly swaps a single color by id
	 */
	private SwapDiff swap(ColorType type, Integer colorId, SwapMode swapMode)
	{
		if (type == null || colorId == null)
		{
			return SwapDiff.blank();
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return SwapDiff.blank();
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return SwapDiff.blank();
		}
		try
		{
			int oldColorId = setColorId(composition, type, colorId);
			Map<ColorType, SwapDiff.Change> changes = new HashMap<>();
			changes.put(type, new SwapDiff.Change(oldColorId, savedSwaps.containsColor(type)));
			switch (swapMode)
			{
				case SAVE:
					savedSwaps.putColor(type, colorId);
					break;
				case REVERT:
					savedSwaps.removeColor(type);
					break;
				case PREVIEW:
					break;
			}
			return new SwapDiff(new HashMap<>(), changes, null, null);
		}
		catch (Exception e)
		{
			return SwapDiff.blank();
		}
	}

	/**
	 * Most of the requested swaps should call this method instead of the more specific swaps below, to ensure
	 * that the swap doesn't result in illegal combinations of models
	 */
	private SwapDiff swap(CompoundSwap s, SwapMode swapMode)
	{
		return swap(s, false, swapMode);
	}

	private SwapDiff swap(CompoundSwap s, SwapMode swapMode, SwapMode iconSwapMode)
	{
		return swap(s, false, (ignore) -> swapMode, iconSwapMode);
	}

	private SwapDiff swap(CompoundSwap s, boolean allowDisabledSwaps, SwapMode swapMode)
	{
		return swap(s, allowDisabledSwaps, (ignore) -> swapMode, swapMode);
	}

	private SwapDiff swap(
		CompoundSwap s,
		boolean allowDisabledSwaps,
		Function<KitType, SwapMode> swapModeProvider,
		SwapMode iconSwapMode)
	{
		switch (s.getType())
		{
			case HEAD:
				Integer headEquipId = s.getEquipmentIds().get(KitType.HEAD);
				Integer hairEquipId = s.getEquipmentIds().get(KitType.HAIR);
				Integer jawEquipId = s.getEquipmentIds().get(KitType.JAW);
				JawIcon icon = s.getIcon();
				return swapHead(headEquipId, hairEquipId, jawEquipId, icon, allowDisabledSwaps, swapModeProvider,
					iconSwapMode);
			case TORSO:
				Integer torsoEquipId = s.getEquipmentIds().get(KitType.TORSO);
				Integer armsEquipId = s.getEquipmentIds().get(KitType.ARMS);
				return swapTorso(torsoEquipId, armsEquipId, allowDisabledSwaps, swapModeProvider);
			case WEAPONS:
				Integer weaponEquipId = s.getEquipmentIds().get(KitType.WEAPON);
				Integer shieldEquipId = s.getEquipmentIds().get(KitType.SHIELD);
				return swapWeapons(weaponEquipId, shieldEquipId, allowDisabledSwaps, swapModeProvider);
			case SINGLE:
				return swapSingle(s.getKitType(), s.getEquipmentId(), allowDisabledSwaps,
					swapModeProvider.apply(s.getKitType()));
		}
		return SwapDiff.blank();
	}

	private SwapDiff swapSingle(KitType slot, Integer equipmentId, boolean allowDisabledSwaps, SwapMode swapMode)
	{
		Map<SwapDiff.Change.Type, SwapDiff.Change> results = swap(slot, equipmentId, swapMode, SwapMode.PREVIEW,
			allowDisabledSwaps);
		Map<KitType, SwapDiff.Change> changes = new HashMap<>();
		SwapDiff.Change iconChange = results.get(SwapDiff.Change.Type.ICON);
		if (results.containsKey(SwapDiff.Change.Type.EQUIPMENT))
		{
			changes.put(slot, results.get(SwapDiff.Change.Type.EQUIPMENT));
		}
		return new SwapDiff(changes, new HashMap<>(), iconChange, null);
	}

	/**
	 * Swaps an item in the head slot, and kits in the hair and/or jaw slots. Behavior changes depending
	 * on the information present:
	 * <p>
	 * When the head slot id is null, the head slot will not change. The hair and jaw kits sent
	 * will be used if the currently shown item allows for it, otherwise nothing will happen.
	 * <p>
	 * The head slot item is removed when id == 0. This means swapping to the hair and jaw kit ids
	 * if they are sent, otherwise the player's existing kit ids will be used.
	 * <p>
	 * For non-null, non-zero head ids, the head item will be swapped, but not if (the item hides hair and the hair kit
	 * is locked) or (the item hides jaws and the jaw slot is locked). If the new item allows showing hair
	 * and/or jaw kits, then they will also be swapped using the sent values or the existing kits on the player.
	 * If the kits are not allowed, they will be hidden as needed.
	 */
	private SwapDiff swapHead(
		Integer headEquipId,
		Integer hairEquipId,
		Integer jawEquipId,
		JawIcon icon,
		boolean allowDisabledSwaps,
		Function<KitType, SwapMode> swapModeProvider,
		SwapMode iconSwapMode)
	{
		// equipment ids that will be used in the swap
		Integer finalHeadId = null;
		Integer finalHairId;
		Integer finalJawId;

		Function<Integer, Boolean> isJawlessIcon = (equipId) ->
			equipId >= ITEM_OFFSET && JawKit.isNoJawIcon(equipId - ITEM_OFFSET);
		Function<Integer, Boolean> headAllowsHair = (equipId) ->
			equipId < ITEM_OFFSET || ItemInteractions.HAIR_HELMS.contains(equipId - ITEM_OFFSET);
		Function<Integer, Boolean> headAllowsJaw = (equipId) ->
			equipId < ITEM_OFFSET || !ItemInteractions.NO_JAW_HELMS.contains(equipId - ITEM_OFFSET);

		int currentHeadEquipId = equipmentIdInSlot(KitType.HEAD);

		Integer adjustedJawEquipId = !savedSwaps.isKitLocked(KitType.JAW) ? jawEquipId : null;
		JawIcon adjustedIcon = !savedSwaps.isIconLocked() ? icon : null;
		if (adjustedJawEquipId != null || icon != null)
		{
			adjustedJawEquipId = combineJawIcon(adjustedJawEquipId, adjustedIcon);
		}

		Integer adjustedHairEquipId = !savedSwaps.isKitLocked(KitType.HAIR) ? hairEquipId : null;

		if (savedSwaps.isItemLocked(KitType.HEAD))
		{
			// only change hair/jaw and only if current head item allows
			if (adjustedHairEquipId != null)
			{
				boolean hairAllowed = (headAllowsHair.apply(currentHeadEquipId) && adjustedHairEquipId > 0) ||
					(!headAllowsHair.apply(currentHeadEquipId) && adjustedHairEquipId <= 0);
				finalHairId = hairAllowed ? adjustedHairEquipId : null;
			}
			else
			{
				finalHairId = null;
			}
			if (adjustedJawEquipId != null)
			{
				boolean jawAllowed = (headAllowsJaw.apply(currentHeadEquipId) && adjustedJawEquipId > 0) ||
					(!headAllowsJaw.apply(currentHeadEquipId) && (adjustedJawEquipId <= 0 ||
						isJawlessIcon.apply(adjustedJawEquipId)));
				if (jawAllowed)
				{
					finalJawId = adjustedJawEquipId;
				}
				else if (adjustedJawEquipId >= ITEM_OFFSET)
				{
					finalJawId = combineJawIcon(0, adjustedIcon);
				}
				else
				{
					finalJawId = null;
				}
			}
			else
			{
				finalJawId = null;
			}
		}
		else if (headEquipId == null)
		{
			// prioritize showing hair/jaw and hiding current helm if new kits conflict with it.
			finalHairId = adjustedHairEquipId;
			finalJawId = adjustedJawEquipId;
			boolean headForbidsHair = finalHairId != null && finalHairId > 0 &&
				!headAllowsHair.apply(currentHeadEquipId);
			if (headForbidsHair)
			{
				finalHeadId = 0;
			}
			else
			{
				boolean headForbidsJaw = finalJawId != null && finalJawId > 0 && !isJawlessIcon.apply(finalJawId) &&
					!headAllowsJaw.apply(currentHeadEquipId);
				if (headForbidsJaw)
				{
					// if not actually requesting jaw kit, use only icon and keep head item
					if (jawEquipId == null)
					{
						finalJawId = combineJawIcon(0, adjustedIcon);
					}
					else
					{
						finalHeadId = 0;
					}
				}
			}
		}
		else
		{
			// priority: show head and hide hair/jaw, but not if locks on hair/jaw disallow (in which case don't change)
			boolean hairDisallowed = !headAllowsHair.apply(headEquipId) && savedSwaps.isKitLocked(KitType.HAIR);
			boolean jawDisallowed = !headAllowsJaw.apply(headEquipId) && savedSwaps.isKitLocked(KitType.JAW);
			finalHeadId = hairDisallowed || jawDisallowed ? null : headEquipId;
			Integer headIdToCheck = hairDisallowed || jawDisallowed ? currentHeadEquipId : headEquipId;
			Integer potentialHairId = headAllowsHair.apply(headIdToCheck) ? adjustedHairEquipId : Integer.valueOf(0);
			finalHairId = !savedSwaps.isKitLocked(KitType.HAIR) ? potentialHairId : null;
			Integer potentialJawId = headAllowsJaw.apply(headIdToCheck) ||
				(adjustedJawEquipId != null && isJawlessIcon.apply(adjustedJawEquipId)) ?
				adjustedJawEquipId :
				(Integer) combineJawIcon(0, adjustedIcon);
			finalJawId = !savedSwaps.isKitLocked(KitType.JAW) ? potentialJawId : null;
		}

		Map<KitType, SwapDiff.Change> changes = new HashMap<>();
		final SwapDiff.Change[] iconChange = {null};

		// edge cases:
		// if not changing hair but hair can be shown, make sure that at least something is displayed there
		if (finalHairId == null && equipmentIdInSlot(KitType.HAIR) == 0 &&
			((finalHeadId == null && headAllowsHair.apply(currentHeadEquipId)) ||
				(finalHeadId != null && headAllowsHair.apply(finalHeadId))))
		{
			if (savedSwaps.isKitLocked(KitType.HAIR))
			{
				// hair is locked on nothing, don't change helm to something that could show empty kit
				finalHeadId = null;
				finalJawId = null;
			}
			else
			{
				int revertHairId = savedSwaps.getRealKit(KitType.HAIR, gender) + KIT_OFFSET;
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(KitType.HAIR, revertHairId, SwapMode.REVERT,
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(KitType.HAIR, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		}

		int currentJawEquipId = equipmentIdInSlot(KitType.JAW);
		// if not changing jaw but jaw should be shown, make sure that at least something is displayed there
		if (finalJawId == null && (currentJawEquipId == 0 || isJawlessIcon.apply(currentJawEquipId)) &&
			((finalHeadId == null && headAllowsJaw.apply(currentHeadEquipId)) ||
				(finalHeadId != null && headAllowsJaw.apply(finalHeadId))))
		{
			if (savedSwaps.isKitLocked(KitType.JAW))
			{
				// jaw is locked on nothing, don't change helm to something that could show empty kit
				finalHeadId = null;
				finalHairId = null;
			}
			else
			{
				int realKitId = savedSwaps.getRealKit(KitType.JAW, gender);
				int fallbackJawId = combineJawIcon(realKitId + KIT_OFFSET, adjustedIcon);
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(KitType.JAW, fallbackJawId, SwapMode.REVERT,
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(KitType.JAW, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		}

		BiConsumer<KitType, Integer> attemptChange = (slot, equipId) -> {
			if (equipId != null && equipId >= 0)
			{
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(slot, equipId, swapModeProvider.apply(slot),
					iconSwapMode, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(slot, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
				if (result.containsKey(SwapDiff.Change.Type.ICON))
				{
					iconChange[0] = result.get(SwapDiff.Change.Type.ICON);
				}
			}
		};
		attemptChange.accept(KitType.HEAD, finalHeadId);
		attemptChange.accept(KitType.HAIR, finalHairId);
		attemptChange.accept(KitType.JAW, finalJawId);
		return new SwapDiff(changes, new HashMap<>(), iconChange[0], null);
	}

	/**
	 * Swaps an item or kit in the torso slot and a kit in the arms slot. Behavior changes depending
	 * on the information present:
	 * <p>
	 * When the torso slot id is null, the torso slot will not change. The arms kits sen will be
	 * used if the currently shown torso allows for it, otherwise nothing will happen.
	 * <p>
	 * The torso slot item is removed when id == 0. This means swapping to the arms kit id if it is sent,
	 * otherwise the player's existing kit id will be used.
	 * <p>
	 * For non-null, non-zero torso ids, the torso slot item will change, but not if the item hides arms and the arms
	 * kit is currently locked. If the new item allows showing the arms kit, then it will also be swapped using the
	 * sent value or the existing kit on the player. If the torso does not allow showing arms, then the arms kit
	 * will be removed.
	 */
	private SwapDiff swapTorso(
		Integer torsoEquipId,
		Integer armsEquipId,
		boolean allowDisabledSwaps,
		Function<KitType, SwapMode> swapModeProvider)
	{
		Integer finalTorsoId = null;
		Integer finalArmsId;

		Function<Integer, Boolean> torsoAllowsArms = (equipId) ->
			equipId < ITEM_OFFSET || ItemInteractions.ARMS_TORSOS.contains(equipId - ITEM_OFFSET);

		Map<KitType, SwapDiff.Change> changes = new HashMap<>();

		int currentTorsoEquipId = equipmentIdInSlot(KitType.TORSO);
		if (savedSwaps.isKitLocked(KitType.TORSO) ||
			(savedSwaps.isItemLocked(KitType.TORSO) && torsoEquipId != null && torsoEquipId >= ITEM_OFFSET))
		{
			// only change arms and only if current torso item allows
			if (savedSwaps.isKitLocked(KitType.ARMS) && armsEquipId != null)
			{
				boolean armsAllowed = (torsoAllowsArms.apply(currentTorsoEquipId) && armsEquipId > 0) ||
					(!torsoAllowsArms.apply(currentTorsoEquipId) && armsEquipId <= 0);
				finalArmsId = armsAllowed ? armsEquipId : null;
			}
			else
			{
				finalArmsId = null;
			}
		}
		else if (torsoEquipId == null)
		{
			// prioritize showing arms and hiding current torso if applicable
			finalArmsId = !savedSwaps.isKitLocked(KitType.ARMS) ? armsEquipId : null;
			boolean torsoForbidsArms = !torsoAllowsArms.apply(currentTorsoEquipId) && finalArmsId != null &&
				finalArmsId > 0;
			if (torsoForbidsArms)
			{
				int revertTorsoId = savedSwaps.getRealKit(KitType.TORSO, gender) + KIT_OFFSET;
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(KitType.TORSO, revertTorsoId, SwapMode.REVERT,
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(KitType.TORSO, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		}
		else
		{
			// priority: show torso and hide arms, but not if locks on arms disallow (in which case don't change)
			boolean torsoDisallowed = !torsoAllowsArms.apply(torsoEquipId) && savedSwaps.isKitLocked(KitType.ARMS);
			finalTorsoId = torsoDisallowed ? null : torsoEquipId;
			Integer torsoIdToCheck = torsoDisallowed ? currentTorsoEquipId : torsoEquipId;
			Integer potentialArmsId = torsoAllowsArms.apply(torsoIdToCheck) ? armsEquipId : Integer.valueOf(0);
			finalArmsId = !savedSwaps.isKitLocked(KitType.ARMS) ? potentialArmsId : null;
		}

		// edge case:
		// if not changing arms but arms can be shown, make sure that at least something is displayed there
		if (finalArmsId == null && equipmentIdInSlot(KitType.ARMS) == 0 &&
			((finalTorsoId == null && torsoAllowsArms.apply(currentTorsoEquipId)) ||
				(finalTorsoId != null && torsoAllowsArms.apply(finalTorsoId))))
		{
			if (savedSwaps.isKitLocked(KitType.ARMS))
			{
				// arms locked on nothing, don't change torso to something that could show empty kit
				finalTorsoId = null;
			}
			else
			{
				int revertArmsId = savedSwaps.getRealKit(KitType.ARMS, gender) + KIT_OFFSET;
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(KitType.ARMS, revertArmsId, SwapMode.REVERT,
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(KitType.ARMS, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		}

		BiConsumer<KitType, Integer> attemptChange = (slot, equipId) -> {
			if (equipId != null && equipId >= 0)
			{
				Map<SwapDiff.Change.Type, SwapDiff.Change> result = swap(slot, equipId, swapModeProvider.apply(slot),
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (result.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(slot, result.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		};
		attemptChange.accept(KitType.TORSO, finalTorsoId);
		attemptChange.accept(KitType.ARMS, finalArmsId);
		return new SwapDiff(changes, new HashMap<>(), null, null);
	}

	/**
	 * Swaps an item weapon slot and an item in the shield slot. Behavior changes depending
	 * on the information present:
	 * <p>
	 * If the weapon id is null, only the shield will be considered. If the shield is non-null and
	 * non-zero, and the current weapon is two-handed, then the weapon will be removed.
	 * <p>
	 * If the weapon is non-null, non-zero, and two-handed, the shield will be removed. Otherwise,
	 * the weapon and shield are both swapped if they are respectively non-null.
	 * <p>
	 * Lastly, the idle animation ID will change if the sent weapon id is non-null or if it's determined
	 * that the weapon must be removed.
	 */
	private SwapDiff swapWeapons(
		Integer weaponEquipId,
		Integer shieldEquipId,
		boolean allowDisabledSwaps,
		Function<KitType, SwapMode> swapModeProvider)
	{
		Integer finalWeaponId = null;
		Integer finalShieldId;
		Integer finalAnimId = null;

		Function<Integer, Boolean> weaponForbidsShields = (equipId) -> {
			if (equipId >= ITEM_OFFSET)
			{
				ItemEquipmentStats stats = equipmentStatsFor(equipId - ITEM_OFFSET);
				return stats != null && stats.isTwoHanded();
			}
			return false;
		};

		if (weaponEquipId == null || savedSwaps.isItemLocked(KitType.WEAPON))
		{
			finalShieldId = shieldEquipId;
			if (shieldEquipId != null && weaponForbidsShields.apply(equipmentIdInSlot(KitType.WEAPON)))
			{
				if (!savedSwaps.isItemLocked(KitType.WEAPON))
				{
					finalWeaponId = 0;
					finalAnimId = IdleAnimationID.DEFAULT;
				}
				else
				{
					// weapon is locked and 2h, should not equip shield
					finalShieldId = null;
				}
			}
		}
		else
		{
			finalShieldId = weaponForbidsShields.apply(weaponEquipId) && !savedSwaps.isItemLocked(KitType.SHIELD) ?
				Integer.valueOf(0) :
				shieldEquipId;
			finalWeaponId = weaponForbidsShields.apply(weaponEquipId) && savedSwaps.isItemLocked(KitType.SHIELD) ?
				null :
				weaponEquipId;
			finalAnimId = finalWeaponId != null ?
				ItemInteractions.WEAPON_TO_IDLE.getOrDefault(weaponEquipId - ITEM_OFFSET, IdleAnimationID.DEFAULT) :
				null;
		}

		Map<KitType, SwapDiff.Change> changes = new HashMap<>();
		BiConsumer<KitType, Integer> attemptChange = (slot, equipId) -> {
			if (equipId != null && equipId >= 0)
			{
				Map<SwapDiff.Change.Type, SwapDiff.Change> results = swap(slot, equipId, swapModeProvider.apply(slot),
					SwapMode.PREVIEW, allowDisabledSwaps);
				if (results.containsKey(SwapDiff.Change.Type.EQUIPMENT))
				{
					changes.put(slot, results.get(SwapDiff.Change.Type.EQUIPMENT));
				}
			}
		};
		attemptChange.accept(KitType.WEAPON, finalWeaponId);
		attemptChange.accept(KitType.SHIELD, finalShieldId);

		Integer changedAnim = null;
		if (finalAnimId != null && finalAnimId >= 0)
		{
			Player player = client.getLocalPlayer();
			if (player != null)
			{
				changedAnim = setIdleAnimationId(player, finalAnimId, allowDisabledSwaps);
				if (changedAnim.equals(finalAnimId))
				{
					changedAnim = null;
				}
			}
		}
		return new SwapDiff(changes, new HashMap<>(), null, changedAnim);
	}

	/**
	 * this should only be called from one of the swap methods utilizing `CompoundSwap`, otherwise the
	 * swap may not make logical sense.
	 * <p>
	 * equipmentId follows `PlayerComposition::getEquipmentId`:
	 * 0 for nothing
	 * 256-2047 for a base kit model (i.e., kitId + 256)
	 * >=2048 for an item (i.e., itemId + 2048)
	 * <p>
	 *
	 * @return a map of changes performed, keyed by type (potentially empty if no changes)
	 */
	private Map<SwapDiff.Change.Type, SwapDiff.Change> swap(
		KitType slot, int equipmentId, SwapMode swapMode, SwapMode iconSwapMode, boolean allowDisabledSwaps)
	{
		Map<SwapDiff.Change.Type, SwapDiff.Change> changes = new HashMap<>();
		if (slot == null)
		{
			return changes;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return changes;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return changes;
		}
		// returns equipment id of jaw kit (without icon)
		Function<Integer, Integer> deIconJaw = (equipId) -> {
			JawKit kit = JawKit.fromEquipmentId(equipId);
			if (kit != null)
			{
				Integer kitId = kit.getKitId(gender);
				if (kitId != null)
				{
					return kitId + KIT_OFFSET;
				}
			}
			return equipId;
		};
		// adjust equipment ids for jaw swaps since diff could be icon-only
		int saveId = equipmentId;
		if (slot == KitType.JAW && equipmentId >= ITEM_OFFSET)
		{
			saveId = deIconJaw.apply(equipmentId);
		}
		Supplier<Boolean> unnaturalCheck = () -> savedSwaps.containsSlot(slot) || savedSwaps.isHidden(slot);
		Supplier<Boolean> unnaturalIconCheck = () -> savedSwaps.containsIcon();
		int oldId = setEquipmentId(composition, slot, equipmentId, allowDisabledSwaps);
		int oldSaveId = oldId;
		if (slot == KitType.JAW && oldId >= ITEM_OFFSET)
		{
			oldSaveId = deIconJaw.apply(oldId);
		}
		boolean oldUnnatural = unnaturalCheck.get();
		switch (swapMode)
		{
			case SAVE:
				if (saveId <= 0)
				{
					if (ALLOWS_NOTHING.contains(slot))
					{
						savedSwaps.putNothing(slot);
					}
					else
					{
						savedSwaps.removeSlot(slot);
					}
				}
				else if (saveId < ITEM_OFFSET)
				{
					savedSwaps.putKit(slot, saveId - KIT_OFFSET);
				}
				else
				{
					savedSwaps.putItem(slot, saveId - ITEM_OFFSET);
				}
				break;
			case REVERT:
				savedSwaps.removeSlot(slot);
				break;
			case PREVIEW:
				break;
		}
		if (slot == KitType.JAW)
		{
			boolean oldUnnaturalIcon = unnaturalIconCheck.get();
			JawIcon oldIcon = oldId >= ITEM_OFFSET ? JawKit.iconFromItemId(oldId - ITEM_OFFSET) : JawIcon.NOTHING;
			JawIcon icon = equipmentId >= ITEM_OFFSET ? JawKit.iconFromItemId(equipmentId - ITEM_OFFSET) : JawIcon.NOTHING;
			switch (iconSwapMode)
			{
				case SAVE:
					if (equipmentId < ITEM_OFFSET)
					{
						savedSwaps.removeIcon();
					}
					else
					{
						savedSwaps.putIcon(icon);
					}
					break;
				case REVERT:
					savedSwaps.removeIcon();
					break;
				case PREVIEW:
					break;
			}
			boolean unnaturalIcon = unnaturalIconCheck.get();
			if (icon != oldIcon || oldUnnaturalIcon != unnaturalIcon)
			{
				changes.put(SwapDiff.Change.Type.ICON, new SwapDiff.Change(oldIcon.getId(), oldUnnaturalIcon));
			}
		}
		boolean unnatural = unnaturalCheck.get();
		if (oldSaveId != saveId || oldUnnatural != unnatural)
		{
			changes.put(SwapDiff.Change.Type.EQUIPMENT, new SwapDiff.Change(oldSaveId, oldUnnatural));
		}
		return changes;
	}

	/**
	 * Sets equipment id for slot and returns equipment id of previously occupied item.
	 * If allowDisabledSwaps is false or if the slot is disabled, no actual change occurs.
	 */
	private int setEquipmentId(
		@Nonnull PlayerComposition composition,
		@Nonnull KitType slot,
		int equipmentId,
		boolean allowDisabledSwaps)
	{
		int previousId = composition.getEquipmentIds()[slot.getIndex()];
		if (allowDisabledSwaps || !disabledSlots.containsKey(slot))
		{
			composition.getEquipmentIds()[slot.getIndex()] = equipmentId;
			composition.setHash();
		}
		return previousId;
	}

	/**
	 * Sets color id for slot and returns color id before replacement.
	 */
	private int setColorId(@Nonnull PlayerComposition composition, @Nonnull ColorType type, int colorId)
	{
		int[] colors = composition.getColors();
		int previousId = colors[type.ordinal()];
		colors[type.ordinal()] = colorId;
		composition.setHash();
		return previousId;
	}

	/**
	 * Sets idle animation id for current player and returns previous idle animation.
	 * If allowDisabledSwaps is false or the weapon slot is disabled, no actual anim change occurs.
	 */
	private int setIdleAnimationId(@Nonnull Player player, int animationId, boolean allowDisabledSwaps)
	{
		int previousId = player.getIdlePoseAnimation();
		if (allowDisabledSwaps || !disabledSlots.containsKey(KitType.WEAPON))
		{
			player.setIdlePoseAnimation(animationId);
		}
		return previousId;
	}

	/**
	 * Performs a revert back to an "original" state for the given slot.
	 * In most cases, this swaps to whatever was actually equipped in the slot. Some exceptions:
	 * When reverting a kit, and an item is actually equipped in a slot that hides that kit, the item will be shown
	 * and the kit will be hidden.
	 */
	private SwapDiff doRevert(KitType slot)
	{
		if (slot == KitType.HAIR)
		{
			Integer headItemId = inventoryItemId(KitType.HEAD);
			if (headItemId != null && headItemId >= 0 && !ItemInteractions.HAIR_HELMS.contains(headItemId))
			{
				return swap(CompoundSwap.single(KitType.HEAD, headItemId + ITEM_OFFSET), SwapMode.REVERT, SwapMode.PREVIEW);
			}
		}
		else if (slot == KitType.JAW)
		{
			Integer headItemId = inventoryItemId(KitType.HEAD);
			if (headItemId != null && headItemId >= 0 && ItemInteractions.NO_JAW_HELMS.contains(headItemId))
			{
				return swap(CompoundSwap.single(KitType.HEAD, headItemId + ITEM_OFFSET), SwapMode.REVERT, SwapMode.PREVIEW);
			}
		}
		else if (slot == KitType.ARMS)
		{
			Integer torsoItemId = inventoryItemId(KitType.TORSO);
			if (torsoItemId != null && torsoItemId >= 0 && !ItemInteractions.ARMS_TORSOS.contains(torsoItemId))
			{
				return swap(CompoundSwap.single(KitType.TORSO, torsoItemId + ITEM_OFFSET), SwapMode.REVERT, SwapMode.PREVIEW);
			}
		}
		Integer originalItemId = inventoryItemId(slot);
		Integer originalKitId = savedSwaps.getRealKit(slot, gender);
		if (originalKitId == -KIT_OFFSET)
		{
			originalKitId = null;
		}
		int equipmentId;
		if (originalItemId != null)
		{
			equipmentId = originalItemId < 0 ? 0 : originalItemId + ITEM_OFFSET;
		}
		else if (originalKitId != null)
		{
			equipmentId = originalKitId < 0 ? 0 : originalKitId + KIT_OFFSET;
		}
		else
		{
			equipmentId = 0;
		}
		return swap(CompoundSwap.single(slot, equipmentId), SwapMode.REVERT, SwapMode.PREVIEW);
	}

	private SwapDiff doRevert(ColorType type)
	{
		Integer originalColorId = savedSwaps.getRealColor(type);
		return swap(type, originalColorId, SwapMode.REVERT);
	}

	private SwapDiff doRevertIcon()
	{
		JawIcon revertIcon = savedSwaps.getRealIcon();
		if (revertIcon == null)
		{
			revertIcon = JawIcon.NOTHING;
		}
		int jawEquipId = combineJawIcon(null, revertIcon);
		JawKit kit = JawKit.fromEquipmentId(jawEquipId);
		if (kit != null)
		{
			Integer kitId = kit.getKitId(gender);
			if (kitId != null)
			{
				Map<SwapDiff.Change.Type, SwapDiff.Change> changes = swap(KitType.JAW, kitId + KIT_OFFSET,
					SwapMode.PREVIEW, SwapMode.REVERT, false);
				SwapDiff.Change iconChange = changes.get(SwapDiff.Change.Type.ICON);
				return new SwapDiff(new HashMap<>(), new HashMap<>(), iconChange, null);
			}
		}
		return SwapDiff.blank();
	}

	/**
	 * Combines given jaw equipment id and icon to one equipment id. If either are null, will use current saved
	 * values.
	 */
	private int combineJawIcon(Integer jawEquipId, @Nullable JawIcon icon)
	{
		int finalJawId;
		if (jawEquipId != null)
		{
			finalJawId = jawEquipId;
		}
		else
		{
			Integer jawKitId = savedSwaps.getKit(KitType.JAW);
			if (jawKitId != null)
			{
				finalJawId = jawKitId + KIT_OFFSET;
			}
			else
			{
				finalJawId = savedSwaps.getRealKit(KitType.JAW, gender) + KIT_OFFSET;
			}
		}
		JawIcon finalJawIcon;
		if (icon != null)
		{
			finalJawIcon = icon;
		}
		else
		{
			finalJawIcon = savedSwaps.getSwappedIcon();
			if (finalJawIcon == null)
			{
				finalJawIcon = savedSwaps.getRealIcon();
			}
			if (finalJawIcon == null)
			{
				finalJawIcon = JawIcon.NOTHING;
			}
		}
		JawKit kit = JawKit.fromEquipmentId(finalJawId);
		if (kit != null)
		{
			Integer itemId = kit.getIconItemId(finalJawIcon);
			if (itemId != null)
			{
				return itemId + ITEM_OFFSET;
			}
		}
		return finalJawId;
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

	/**
	 * @return true if the slot does not have an item (real or virtual) obscuring it (directly or indirectly)
	 */
	private boolean isOpen(KitType slot)
	{
		if (slot == null || savedSwaps.isKitLocked(slot) || savedSwaps.containsItem(slot))
		{
			return false;
		}
		Supplier<Boolean> fallback = () -> {
			if (equipmentIdInSlot(slot) > 0)
			{
				// equipment id must be a kit since there can't be an item at this point
				return true;
			}
			else
			{
				Integer actualEquipId = inventoryItemId(slot);
				return actualEquipId == null || actualEquipId < ITEM_OFFSET;
			}
		};
		switch (slot)
		{
			case HAIR:
			case JAW:
				int headEquipId = equipmentIdInSlot(KitType.HEAD);
				if (headEquipId > ITEM_OFFSET)
				{
					int headItemId = headEquipId - ITEM_OFFSET;
					if (slot == KitType.HAIR)
					{
						return ItemInteractions.HAIR_HELMS.contains(headItemId);
					}
					return !ItemInteractions.NO_JAW_HELMS.contains(headItemId);
				}
				return fallback.get();
			case ARMS:
				int torsoEquipId = equipmentIdInSlot(KitType.TORSO);
				if (torsoEquipId > ITEM_OFFSET)
				{
					int torsoItemId = torsoEquipId - ITEM_OFFSET;
					return ItemInteractions.ARMS_TORSOS.contains(torsoItemId);
				}
				return fallback.get();
			default:
				return fallback.get();
		}
	}

	@Nullable
	private ItemEquipmentStats equipmentStatsFor(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId);
		return stats != null && stats.isEquipable() ? stats.getEquipment() : null;
	}

	/**
	 * returns the equipment id of whatever is being displayed in the given slot
	 */
	private int equipmentIdInSlot(KitType kitType)
	{
		if (savedSwaps.isHidden(kitType))
		{
			return 0;
		}
		Integer virtualItemId = savedSwaps.getItem(kitType);
		if (virtualItemId != null && virtualItemId >= 0)
		{
			return virtualItemId + ITEM_OFFSET;
		}
		Integer realItemId = inventoryItemId(kitType);
		if (realItemId != null && realItemId >= 0)
		{
			// check if a virtual slot is obscuring the real item
			boolean realItemHidden = false;
			switch (kitType)
			{
				case HEAD:
					realItemHidden = (!ItemInteractions.HAIR_HELMS.contains(realItemId) &&
						savedSwaps.containsSlot(KitType.HAIR)) ||
						(ItemInteractions.NO_JAW_HELMS.contains(realItemId) && savedSwaps.containsSlot(KitType.JAW));
					break;
				case TORSO:
					realItemHidden = !ItemInteractions.ARMS_TORSOS.contains(realItemId) &&
						savedSwaps.containsSlot(KitType.ARMS);
					break;
				default:
					break;
			}
			if (!realItemHidden)
			{
				return realItemId + ITEM_OFFSET;
			}
		}
		if (kitType == KitType.JAW)
		{
			int jawEquipId = combineJawIcon(null, null);
			int headEquipId = equipmentIdInSlot(KitType.HEAD);
			boolean jawlessIcon = jawEquipId >= ITEM_OFFSET && JawKit.isNoJawIcon(jawEquipId - ITEM_OFFSET);
			if (!ItemInteractions.NO_JAW_HELMS.contains(headEquipId - ITEM_OFFSET) || jawEquipId <= 0 || jawlessIcon)
			{
				return jawEquipId;
			}
		}
		Integer kitId = kitIdFor(kitType);
		return kitId != null && kitId >= 0 ? kitId + KIT_OFFSET : 0;
	}

	/**
	 * returns the item id of the actual item equipped in the given slot (swaps are ignored)
	 */
	@Nullable
	public Integer inventoryItemId(KitType kitType)
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
		return item != null && item.getId() >= 0 ? item.getId() : null;
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
		return composition == null ? null : composition.getKitId(kitType);
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
		int[] colors = composition.getColors();
		return colors[colorType.ordinal()];
	}

	// restores diff and returns a new diff with reverted changes (allows redo)
	private SwapDiff restore(SwapDiff swapDiff, boolean save)
	{
		Function<KitType, SwapMode> swapModeProvider = (slot) -> {
			SwapDiff.Change change = swapDiff.getSlotChanges().get(slot);
			return !save ? SwapMode.PREVIEW : change != null && change.isUnnatural() ?
				SwapMode.SAVE :
				SwapMode.REVERT;
		};
		SwapDiff.Change iconChange = swapDiff.getIconChange();
		SwapMode iconSwapMode = (!save || iconChange == null) ? SwapMode.PREVIEW :
			iconChange.isUnnatural() ? SwapMode.SAVE : SwapMode.REVERT;
		JawIcon icon = iconChange != null ? JawIcon.fromId(iconChange.getId()) : null;
		// restore kits and items
		Map<KitType, Integer> restoreEquipIds = sanitize(
			swapDiff.getSlotChanges().entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()))
		);
		SwapDiff slotRestore = CompoundSwap.fromMap(restoreEquipIds, icon)
			.stream()
			.map(c -> this.swap(c, false, swapModeProvider, iconSwapMode))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		SwapDiff colorRestore = swapDiff.getColorChanges().entrySet()
			.stream()
			.map(e -> {
				ColorType type = e.getKey();
				SwapDiff.Change change = e.getValue();
				int colorId = change.getId();
				SwapMode mode;
				if (save)
				{
					mode = change.isUnnatural() ? SwapMode.SAVE : SwapMode.REVERT;
				}
				else
				{
					mode = SwapMode.PREVIEW;
				}
				return swap(type, colorId, mode);
			})
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		return slotRestore.mergeOver(colorRestore);
	}

	@Nullable
	private KitType itemSlotMatch(String name)
	{
		try
		{
			return KitType.valueOf(name);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Nullable
	private KitType kitSlotMatch(String name)
	{
		try
		{
			String k = name.replace(KIT_SUFFIX, "");
			return KitType.valueOf(k);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Nullable
	private ColorType colorSlotMatch(String name)
	{
		try
		{
			String c = name.replace(COLOR_SUFFIX, "");
			return ColorType.valueOf(c);
		}
		catch (Exception e)
		{
			return null;
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

}
