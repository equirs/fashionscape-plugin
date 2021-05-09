package eq.uirs.fashionscape.swap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.data.ColorType;
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
import org.apache.commons.lang3.SerializationUtils;

/**
 * observable wrapper for user's item swaps. Persists swaps to plugin config.
 */
@Singleton
@Slf4j
class SavedSwaps
{
	private static final int DEBOUNCE_DELAY_MS = 200;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private ScheduledExecutorService executor;

	private final HashMap<KitType, Integer> swappedItemIds = new HashMap<>();
	private final HashMap<KitType, Integer> swappedKitIds = new HashMap<>();
	private final HashMap<ColorType, Integer> swappedColorIds = new HashMap<>();
	// currently only applicable for slots that exclusively hold items
	@Getter
	private final HashSet<KitType> hiddenSlots = new HashSet<>();

	private final Set<KitType> lockedKits = new HashSet<>();
	// If kit is locked, item must also be locked. If item is unlocked, kit must also be unlocked.
	// Only valid state where locks differ is item locked, kit unlocked
	private final Set<KitType> lockedItems = new HashSet<>();
	private final Set<ColorType> lockedColors = new HashSet<>();

	private final Map<String, List<SwapEventListener<? extends SwapEvent>>> listeners = new HashMap<>();

	// should only load once since config might be behind local state
	private boolean hasLoadedConfig = false;
	private Future<?> colorSaveFuture = null;
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

	boolean containsSlot(KitType slot)
	{
		return swappedKitIds.containsKey(slot) || swappedItemIds.containsKey(slot);
	}

	boolean containsItem(KitType slot)
	{
		return swappedItemIds.containsKey(slot);
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
		Integer oldId = swappedItemIds.put(slot, itemId);
		hiddenSlots.remove(slot);
		if (!itemId.equals(oldId))
		{
			fireEvent(new ItemChanged(slot, itemId));
		}
		if (swappedKitIds.containsKey(slot))
		{
			removeKit(slot);
		}
		saveEquipmentConfigDebounced();
	}

	// this differs from removing, which leaves the slot open for the real item/kit to show
	void putNothing(KitType slot)
	{
		// todo bug: equip hair/jar kit, clear "nothing" in head, equip helm, voila clipping...
		if (lockedItems.contains(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.remove(slot);
		boolean wasNotHidden = hiddenSlots.add(slot);
		if (oldId != null || wasNotHidden)
		{
			fireEvent(new ItemChanged(slot, -1));
		}
		saveEquipmentConfigDebounced();
	}

	void putKit(KitType slot, Integer kitId)
	{
		if (lockedKits.contains(slot))
		{
			return;
		}
		Integer oldId = swappedKitIds.put(slot, kitId);
		if (!kitId.equals(oldId))
		{
			fireEvent(new KitChanged(slot, kitId));
		}
		if (swappedItemIds.containsKey(slot))
		{
			removeItem(slot);
		}
		saveEquipmentConfigDebounced();
	}

	void putColor(ColorType type, Integer colorId)
	{
		Integer oldId = swappedColorIds.put(type, colorId);
		if (!colorId.equals(oldId))
		{
			fireEvent(new ColorChanged(type, colorId));
		}
		saveColorConfigDebounced();
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
		}
		saveEquipmentConfigDebounced();
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
		}
		saveEquipmentConfigDebounced();
	}

	void removeColor(ColorType type)
	{
		Integer oldId = swappedColorIds.remove(type);
		if (oldId != null)
		{
			fireEvent(new ColorChanged(type, null));
		}
		saveColorConfigDebounced();
	}

	void clear()
	{
		Set<KitType> swappedItems = Sets.union(new HashSet<>(swappedItemIds.keySet()), new HashSet<>(hiddenSlots));
		Set<KitType> itemSlots = Sets.difference(swappedItems, lockedItems);
		itemSlots.forEach(this::removeItem);
		Set<KitType> kitSlots = Sets.difference(new HashSet<>(swappedKitIds.keySet()), lockedKits);
		kitSlots.forEach(this::removeKit);
		Set<ColorType> colorTypes = Sets.difference(new HashSet<>(swappedColorIds.keySet()), lockedColors);
		colorTypes.forEach(this::removeColor);
	}

	boolean isSlotLocked(KitType slot)
	{
		return lockedKits.contains(slot) || lockedItems.contains(slot);
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

	void removeAllLocks()
	{
		Set<KitType> slotClears = new HashSet<>(lockedKits);
		slotClears.addAll(lockedItems);
		Set<ColorType> colorClears = new HashSet<>(lockedColors);
		lockedKits.clear();
		lockedItems.clear();
		lockedColors.clear();
		slotClears.forEach(slot -> fireEvent(new LockChanged(slot, false, LockChanged.Type.BOTH)));
		colorClears.forEach(type -> fireEvent(new ColorLockChanged(type, false)));
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

	private void fireEvent(SwapEvent event)
	{
		String key = event.getKey();
		listeners.getOrDefault(key, new LinkedList<>()).forEach(listener -> listener.onEvent(event));
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

}
