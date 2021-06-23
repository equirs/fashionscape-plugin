package eq.uirs.fashionscape.swap;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.kit.ArmsKit;
import eq.uirs.fashionscape.data.kit.BootsKit;
import eq.uirs.fashionscape.data.kit.HairKit;
import eq.uirs.fashionscape.data.kit.HandsKit;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.LegsKit;
import eq.uirs.fashionscape.data.kit.TorsoKit;
import eq.uirs.fashionscape.swap.event.ColorChanged;
import eq.uirs.fashionscape.swap.event.ColorLockChanged;
import eq.uirs.fashionscape.swap.event.IconChanged;
import eq.uirs.fashionscape.swap.event.IconLockChanged;
import eq.uirs.fashionscape.swap.event.ItemChanged;
import eq.uirs.fashionscape.swap.event.KitChanged;
import eq.uirs.fashionscape.swap.event.KnownKitChanged;
import eq.uirs.fashionscape.swap.event.LockChanged;
import eq.uirs.fashionscape.swap.event.SwapEvent;
import eq.uirs.fashionscape.swap.event.SwapEventListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import org.apache.commons.lang3.SerializationUtils;

/**
 * observable wrapper for user's item swaps. Persists swaps to plugin config.
 */
@Singleton
@Slf4j
class SavedSwaps
{
	private static final int DEBOUNCE_DELAY_MS = 500;
	// when player's kit info is not known, fall back to showing some default values
	private static final Map<KitType, Integer> FALLBACK_MALE_KITS = new HashMap<>();
	private static final Map<KitType, Integer> FALLBACK_FEMALE_KITS = new HashMap<>();

	static
	{
		FALLBACK_MALE_KITS.put(KitType.HAIR, HairKit.BALD.getKitId());
		FALLBACK_MALE_KITS.put(KitType.JAW, JawKit.GOATEE.getKitId());
		FALLBACK_MALE_KITS.put(KitType.TORSO, TorsoKit.PLAIN.getKitId());
		FALLBACK_MALE_KITS.put(KitType.ARMS, ArmsKit.REGULAR.getKitId());
		FALLBACK_MALE_KITS.put(KitType.LEGS, LegsKit.PLAIN_L.getKitId());
		FALLBACK_MALE_KITS.put(KitType.HANDS, HandsKit.PLAIN_H.getKitId());
		FALLBACK_MALE_KITS.put(KitType.BOOTS, BootsKit.SMALL.getKitId());

		FALLBACK_FEMALE_KITS.put(KitType.HAIR, HairKit.PIGTAILS.getKitId());
		FALLBACK_FEMALE_KITS.put(KitType.JAW, -256);
		FALLBACK_FEMALE_KITS.put(KitType.TORSO, TorsoKit.SIMPLE.getKitId());
		FALLBACK_FEMALE_KITS.put(KitType.ARMS, ArmsKit.SHORT_SLEEVES.getKitId());
		FALLBACK_FEMALE_KITS.put(KitType.LEGS, LegsKit.PLAIN_LF.getKitId());
		FALLBACK_FEMALE_KITS.put(KitType.HANDS, HandsKit.PLAIN_HF.getKitId());
		FALLBACK_FEMALE_KITS.put(KitType.BOOTS, BootsKit.SMALL_F.getKitId());
	}

	@Inject
	private FashionscapeConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ScheduledExecutorService executor;

	// player's real kit ids, e.g., hairstyles, base clothing
	private final HashMap<KitType, Integer> realKitIds = new HashMap<>();
	// player's real base colors
	private final Map<ColorType, Integer> realColorIds = new HashMap<>();
	// player's real jaw icon (derived from real jaw item)
	@Getter
	private JawIcon realIcon = JawIcon.NOTHING;

	private final HashMap<KitType, Integer> swappedItemIds = new HashMap<>();
	private final HashMap<KitType, Integer> swappedKitIds = new HashMap<>();
	private final HashMap<ColorType, Integer> swappedColorIds = new HashMap<>();
	// swapped icon is stored as item id
	@Getter
	private JawIcon swappedIcon = null;
	// currently only applicable for slots that exclusively hold items
	@Getter
	private final HashSet<KitType> hiddenSlots = new HashSet<>();

	private final Set<KitType> lockedKits = new HashSet<>();
	// If kit is locked, item must also be locked. If item is unlocked, kit must also be unlocked.
	// Only valid state where locks differ is item locked, kit unlocked
	private final Set<KitType> lockedItems = new HashSet<>();
	private final Set<ColorType> lockedColors = new HashSet<>();
	private boolean lockedIcon = false;

	private final Map<String, List<SwapEventListener<? extends SwapEvent>>> listeners = new HashMap<>();

	// should only load once since config might be behind local state
	private boolean hasLoadedConfig = false;
	private Future<?> colorSaveFuture = null;
	private Future<?> kitsSaveFuture = null;
	private Future<?> equipSaveFuture = null;

	/**
	 * Reads saved swaps from config. Should be called after listeners are set (i.e., after panels created)
	 */
	void loadFromConfig()
	{
		if (!hasLoadedConfig)
		{
			hasLoadedConfig = true;
		}
		else
		{
			return;
		}
		byte[] equipment = config.currentEquipment();
		byte[] colors = config.currentColors();
		Integer iconId = config.currentIcon();
		try
		{
			Map<KitType, Integer> equipIds = SerializationUtils.deserialize(equipment);
			equipIds.forEach((slot, equipId) -> {
				if (equipId >= 256 && equipId < 512)
				{
					putKit(slot, equipId - 256);
				}
				else if (equipId >= 512)
				{
					putItem(slot, equipId - 512);
				}
			});
			Map<ColorType, Integer> colorIds = SerializationUtils.deserialize(colors);
			colorIds.forEach(this::putColor);
			JawIcon icon = JawIcon.fromId(iconId);
			if (icon != null)
			{
				putIcon(icon);
			}
		}
		catch (Exception ignored)
		{
			// ignore
		}
	}

	/**
	 * Loads default kits from config. Called during pre-refresh check, on first login and on rsn change
	 */
	void loadRSProfileConfig()
	{
		byte[] realKits = configManager.getRSProfileConfiguration(FashionscapeConfig.GROUP,
			FashionscapeConfig.KEY_REAL_KITS, byte[].class);
		try
		{
			realKitIds.putAll(SerializationUtils.deserialize(realKits));
		}
		catch (Exception ignored)
		{
			// ignore
		}
	}

	void addEventListener(SwapEventListener<? extends SwapEvent> listener)
	{
		String key = listener.getKey();
		List<SwapEventListener<?>> list = listeners.getOrDefault(key, new LinkedList<>());
		list.add(listener);
		listeners.put(key, list);
	}

	void removeListeners()
	{
		listeners.clear();
	}

	Set<Map.Entry<KitType, Integer>> itemEntries()
	{
		return ImmutableSet.copyOf(swappedItemIds.entrySet());
	}

	Set<Map.Entry<KitType, Integer>> hiddenSlotEntries()
	{
		return ImmutableSet.copyOf(
			hiddenSlots.stream()
				.collect(Collectors.toMap(v -> v, v -> 0))
				.entrySet()
		);
	}

	Set<Map.Entry<KitType, Integer>> kitEntries()
	{
		return ImmutableSet.copyOf(swappedKitIds.entrySet());
	}

	Set<Map.Entry<ColorType, Integer>> colorEntries()
	{
		return ImmutableSet.copyOf(swappedColorIds.entrySet());
	}

	Integer getItem(KitType slot)
	{
		return swappedItemIds.get(slot);
	}

	Integer getKit(KitType slot)
	{
		return swappedKitIds.get(slot);
	}

	Integer getColor(ColorType type)
	{
		return swappedColorIds.get(type);
	}

	Integer getRealColor(ColorType type)
	{
		return realColorIds.get(type);
	}

	/**
	 * Returns the player's actual kit id in the given slot, or null if it's not known.
	 */
	Integer getRealKit(KitType slot)
	{
		return realKitIds.get(slot);
	}

	/**
	 * Returns the player's actual kit id in the given slot, or a fallback kit id if it's not known
	 */
	int getRealKit(KitType slot, Boolean isFemale)
	{
		Integer realKit = getRealKit(slot);
		return realKit != null ? realKit : getFallbackKit(slot, isFemale);
	}

	int getFallbackKit(KitType slot, Boolean isFemale)
	{
		if (isFemale == null || slot == null)
		{
			return -256;
		}
		Map<KitType, Integer> map = isFemale ? FALLBACK_FEMALE_KITS : FALLBACK_MALE_KITS;
		int result = map.getOrDefault(slot, -256);
		if (result != -256)
		{
			fireEvent(new KnownKitChanged(true, slot));
		}
		return result;
	}

	boolean containsSlot(KitType slot)
	{
		if (swappedKitIds.containsKey(slot))
		{
			return true;
		}
		return containsItem(slot);
	}

	boolean containsItem(KitType slot)
	{
		return swappedItemIds.containsKey(slot);
	}

	boolean containsIcon()
	{
		return swappedIcon != null;
	}

	boolean isHidden(KitType slot)
	{
		return hiddenSlots.contains(slot);
	}

	boolean containsColor(ColorType type)
	{
		return swappedColorIds.containsKey(type);
	}

	void putItem(KitType slot, Integer itemId)
	{
		if (isSlotLocked(slot))
		{
			return;
		}
		if (slot == KitType.JAW)
		{
			log.warn("putting jaw {} - this should not happen", itemId);
		}
		Integer oldId = swappedItemIds.put(slot, itemId);
		hiddenSlots.remove(slot);
		if (swappedKitIds.containsKey(slot))
		{
			removeKit(slot);
		}
		if (!itemId.equals(oldId))
		{
			fireEvent(new ItemChanged(slot, itemId));
			saveEquipmentConfigDebounced();
		}
	}

	void putIcon(JawIcon icon)
	{
		Integer previousId = swappedIcon != null ? swappedIcon.getId() : null;
		Integer newId = icon != null ? icon.getId() : null;
		swappedIcon = icon;
		if (!Objects.equal(previousId, newId))
		{
			fireEvent(new IconChanged(icon));
			saveEquipmentConfigDebounced();
		}
	}

	// this differs from removing, which leaves the slot open for the real item/kit to show
	void putNothing(KitType slot)
	{
		if (lockedItems.contains(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.remove(slot);
		boolean wasNotHidden = hiddenSlots.add(slot);
		if (oldId != null || wasNotHidden)
		{
			fireEvent(new ItemChanged(slot, -1));
			saveEquipmentConfigDebounced();
		}
	}

	void putKit(KitType slot, Integer kitId)
	{
		if (lockedKits.contains(slot))
		{
			return;
		}
		Integer oldId = swappedKitIds.put(slot, kitId);
		if (swappedItemIds.containsKey(slot))
		{
			removeItem(slot);
		}
		if (!kitId.equals(oldId))
		{
			fireEvent(new KitChanged(slot, kitId));
			saveEquipmentConfigDebounced();
		}
	}

	void putColor(ColorType type, Integer colorId)
	{
		Integer oldId = swappedColorIds.put(type, colorId);
		if (!colorId.equals(oldId))
		{
			fireEvent(new ColorChanged(type, colorId));
			saveColorConfigDebounced();
		}
	}

	void putRealKit(KitType slot, Integer kitId)
	{
		fireEvent(new KnownKitChanged(false, slot));
		Integer oldKitId = realKitIds.put(slot, kitId);
		if (!kitId.equals(oldKitId))
		{
			saveRealKitsDebounced();
		}
	}

	void putRealColor(ColorType type, Integer colorId)
	{
		realColorIds.put(type, colorId);
	}

	void putRealIcon(JawIcon icon)
	{
		realIcon = icon;
	}

	void removeSlot(KitType slot)
	{
		removeItem(slot);
		removeKit(slot);
	}

	private void removeItem(KitType slot)
	{
		if (isItemLocked(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.remove(slot);
		boolean wasHidden = hiddenSlots.remove(slot);
		if (oldId != null || wasHidden)
		{
			fireEvent(new ItemChanged(slot, null));
			saveEquipmentConfigDebounced();
		}
	}

	private void removeKit(KitType slot)
	{
		if (isKitLocked(slot))
		{
			return;
		}
		Integer oldId = swappedKitIds.remove(slot);
		if (oldId != null)
		{
			fireEvent(new KitChanged(slot, null));
			saveEquipmentConfigDebounced();
		}
	}

	void removeColor(ColorType type)
	{
		Integer oldId = swappedColorIds.remove(type);
		if (oldId != null)
		{
			fireEvent(new ColorChanged(type, null));
			saveColorConfigDebounced();
		}
	}

	void removeIcon()
	{
		if (swappedIcon != null)
		{
			swappedIcon = null;
			fireEvent(new IconChanged(null));
			saveEquipmentConfigDebounced();
		}
	}

	void clearSwapped()
	{
		Set<KitType> swappedItems = Sets.union(new HashSet<>(swappedItemIds.keySet()), new HashSet<>(hiddenSlots));
		Set<KitType> itemSlots = Sets.difference(swappedItems, lockedItems);
		itemSlots.forEach(this::removeItem);
		Set<KitType> kitSlots = Sets.difference(new HashSet<>(swappedKitIds.keySet()), lockedKits);
		kitSlots.forEach(this::removeKit);
		Set<ColorType> colorTypes = Sets.difference(new HashSet<>(swappedColorIds.keySet()), lockedColors);
		colorTypes.forEach(this::removeColor);
		removeIcon();
	}

	void clearRealKits()
	{
		boolean realKitsNeedRefresh = realKitIds.isEmpty();
		realKitIds.clear();
		if (realKitsNeedRefresh)
		{
			saveRealKitsImmediate();
		}
	}

	Set<KitType> getAllLockedKits()
	{
		return Arrays.stream(KitType.values())
			.filter(lockedKits::contains)
			.collect(Collectors.toSet());
	}

	void setLockedKits(Set<KitType> kits)
	{
		for (KitType slot : KitType.values())
		{
			if (lockedKits.contains(slot) && !kits.contains(slot))
			{
				lockedKits.remove(slot);
				fireEvent(new LockChanged(slot, false, LockChanged.Type.KIT));
			}
			else if (!lockedKits.contains(slot) && kits.contains(slot))
			{
				lockedKits.add(slot);
				fireEvent(new LockChanged(slot, true, LockChanged.Type.KIT));
			}
		}
	}

	Set<KitType> getAllLockedItems()
	{
		return Arrays.stream(KitType.values())
			.filter(lockedItems::contains)
			.collect(Collectors.toSet());
	}

	void setLockedItems(Set<KitType> items)
	{
		for (KitType slot : KitType.values())
		{
			if (lockedItems.contains(slot) && !items.contains(slot))
			{
				lockedItems.remove(slot);
				fireEvent(new LockChanged(slot, false, LockChanged.Type.ITEM));
			}
			else if (!lockedItems.contains(slot) && items.contains(slot))
			{
				lockedItems.add(slot);
				fireEvent(new LockChanged(slot, true, LockChanged.Type.ITEM));
			}
		}
	}

	void setIconLocked(boolean locked)
	{
		lockedIcon = locked;
	}

	boolean isSlotLocked(KitType slot)
	{
		return isKitLocked(slot) || isItemLocked(slot);
	}

	boolean isKitLocked(KitType slot)
	{
		return lockedKits.contains(slot);
	}

	boolean isItemLocked(KitType slot)
	{
		return lockedItems.contains(slot);
	}

	boolean isColorLocked(ColorType type)
	{
		return lockedColors.contains(type);
	}

	boolean isIconLocked()
	{
		return lockedIcon;
	}

	void toggleItemLocked(KitType slot)
	{
		if (lockedItems.contains(slot))
		{
			lockedItems.remove(slot);
			// if item unlocks, kit must also unlock
			lockedKits.remove(slot);
			fireEvent(new LockChanged(slot, false, LockChanged.Type.KIT));
		}
		else
		{
			lockedItems.add(slot);
		}
		fireEvent(new LockChanged(slot, isItemLocked(slot), LockChanged.Type.ITEM));
	}

	void toggleKitLocked(KitType slot)
	{
		if (lockedKits.contains(slot))
		{
			lockedKits.remove(slot);
		}
		else
		{
			lockedKits.add(slot);
			// if kit is locked, item must be locked too
			lockedItems.add(slot);
			fireEvent(new LockChanged(slot, true, LockChanged.Type.ITEM));
		}
		fireEvent(new LockChanged(slot, isSlotLocked(slot), LockChanged.Type.KIT));
	}

	void toggleColorLocked(ColorType type)
	{
		if (lockedColors.contains(type))
		{
			lockedColors.remove(type);
		}
		else
		{
			lockedColors.add(type);
		}
		fireEvent(new ColorLockChanged(type, isColorLocked(type)));
	}

	void toggleIconLocked()
	{
		lockedIcon = !lockedIcon;
		fireEvent(new IconLockChanged(isIconLocked()));
	}

	void removeAllLocks()
	{
		Set<KitType> slotClears = new HashSet<>(lockedKits);
		slotClears.addAll(lockedItems);
		Set<ColorType> colorClears = new HashSet<>(lockedColors);
		lockedKits.clear();
		lockedItems.clear();
		lockedColors.clear();
		lockedIcon = false;
		slotClears.forEach(slot -> fireEvent(new LockChanged(slot, false, LockChanged.Type.BOTH)));
		colorClears.forEach(type -> fireEvent(new ColorLockChanged(type, false)));
		fireEvent(new IconLockChanged(false));
	}

	void removeSlotLock(KitType slot)
	{
		lockedKits.remove(slot);
		lockedItems.remove(slot);
		fireEvent(new LockChanged(slot, false, LockChanged.Type.BOTH));
	}

	void removeColorLock(ColorType type)
	{
		lockedColors.remove(type);
		fireEvent(new ColorLockChanged(type, false));
	}

	void removeIconLock()
	{
		lockedIcon = false;
		fireEvent(new IconLockChanged(false));
	}

	private void fireEvent(SwapEvent event)
	{
		String key = event.getKey();
		listeners.getOrDefault(key, new LinkedList<>()).forEach(listener -> listener.onEvent(event));
	}

	// this needs to be immediate so that changes are reflected when loading from config
	void saveRealKitsImmediate()
	{
		byte[] bytes = SerializationUtils.serialize(realKitIds);
		configManager.setRSProfileConfiguration(FashionscapeConfig.GROUP, FashionscapeConfig.KEY_REAL_KITS, bytes);
	}

	private void saveEquipmentConfigDebounced()
	{
		Future<?> future = equipSaveFuture;
		if (future != null)
		{
			future.cancel(false);
		}
		equipSaveFuture = executor.schedule(() -> {
			Map<KitType, Integer> itemEquips = swappedItemIds.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + 512));
			Map<KitType, Integer> kitEquips = swappedKitIds.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + 256));
			Map<KitType, Integer> hides = hiddenSlots.stream()
				.collect(Collectors.toMap(v -> v, v -> 0));
			HashMap<KitType, Integer> equips = new HashMap<>(itemEquips);
			equips.putAll(kitEquips);
			equips.putAll(hides);
			byte[] bytes = SerializationUtils.serialize(equips);
			config.setCurrentEquipment(bytes);
			Integer iconId = swappedIcon != null ? swappedIcon.getId() : null;
			config.setCurrentIcon(iconId);
		}, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	private void saveColorConfigDebounced()
	{
		Future<?> future = colorSaveFuture;
		if (future != null)
		{
			future.cancel(false);
		}
		colorSaveFuture = executor.schedule(() -> {
			byte[] bytes = SerializationUtils.serialize(swappedColorIds);
			config.setCurrentColors(bytes);
		}, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	private void saveRealKitsDebounced()
	{
		Future<?> future = kitsSaveFuture;
		if (future != null)
		{
			future.cancel(false);
		}
		kitsSaveFuture = executor.schedule(() -> {
			byte[] bytes = SerializationUtils.serialize(realKitIds);
			configManager.setRSProfileConfiguration(FashionscapeConfig.GROUP, FashionscapeConfig.KEY_REAL_KITS, bytes);
		}, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

}
