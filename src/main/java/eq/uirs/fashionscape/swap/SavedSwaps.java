package eq.uirs.fashionscape.swap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import eq.uirs.fashionscape.data.ColorType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

/**
 * observable wrapper for user's item swaps
 */
@RequiredArgsConstructor
class SavedSwaps
{
	private final Map<KitType, Integer> swappedItemIds = new HashMap<>();
	private final Map<KitType, Integer> swappedKitIds = new HashMap<>();
	private final Map<ColorType, Integer> swappedColorIds = new HashMap<>();

	private final Set<KitType> lockedKits = new HashSet<>();
	// If kit is locked, item must also be locked. If item is unlocked, kit must also be unlocked.
	// Only valid state where locks differ is item locked, kit unlocked
	private final Set<KitType> lockedItems = new HashSet<>();
	private final Set<ColorType> lockedColors = new HashSet<>();

	private final Map<String, List<SwapEventListener<? extends SwapEvent>>> listeners = new HashMap<>();

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

	boolean containsColor(ColorType type)
	{
		return swappedColorIds.containsKey(type);
	}

	Integer getItemOrDefault(KitType slot, Integer fallback)
	{
		return swappedItemIds.getOrDefault(slot, fallback);
	}

	Integer getKitOrDefault(KitType slot, Integer fallback)
	{
		return swappedKitIds.getOrDefault(slot, fallback);
	}

	void putItem(KitType slot, Integer itemId)
	{
		if (isSlotLocked(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.put(slot, itemId);
		if (!itemId.equals(oldId))
		{
			fireEvent(new ItemChanged(slot, itemId));
		}
		if (swappedKitIds.containsKey(slot))
		{
			removeKit(slot);
		}
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
	}

	void putColor(ColorType type, Integer colorId)
	{
		Integer oldId = swappedColorIds.put(type, colorId);
		if (!colorId.equals(oldId))
		{
			fireEvent(new ColorChanged(type, colorId));
		}
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
		if (oldId != null)
		{
			fireEvent(new ItemChanged(slot, null));
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
		}
	}

	void removeColor(ColorType type)
	{
		Integer oldId = swappedColorIds.remove(type);
		if (oldId != null)
		{
			fireEvent(new ColorChanged(type, null));
		}
	}

	void clear()
	{
		Set<KitType> itemSlots = Sets.difference(new HashSet<>(swappedItemIds.keySet()), lockedItems);
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
}
