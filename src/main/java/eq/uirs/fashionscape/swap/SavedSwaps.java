package eq.uirs.fashionscape.swap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import eq.uirs.fashionscape.data.ColorType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

/**
 * observable wrapper for user's item swaps
 */
@RequiredArgsConstructor
class SavedSwaps
{
	private static final List<KitType> HEAD_SLOTS = ImmutableList.of(KitType.HAIR, KitType.HEAD, KitType.JAW);
	private static final List<KitType> CHEST_SLOTS = ImmutableList.of(KitType.TORSO, KitType.ARMS);

	private final Map<KitType, Integer> swappedItemIds = new HashMap<>();
	private final Map<KitType, Integer> swappedKitIds = new HashMap<>();
	private final Map<ColorType, Integer> swappedColorIds = new HashMap<>();

	private final Set<KitType> lockedSlots = new HashSet<>();

	private final List<BiConsumer<KitType, Integer>> itemChangeListeners = new LinkedList<>();
	private final List<BiConsumer<KitType, Integer>> kitChangeListeners = new LinkedList<>();
	private final List<BiConsumer<ColorType, Integer>> colorChangeListeners = new LinkedList<>();
	private final List<BiConsumer<KitType, Boolean>> lockListeners = new LinkedList<>();

	void addItemSwapListener(BiConsumer<KitType, Integer> listener)
	{
		itemChangeListeners.add(listener);
	}

	void addKitSwapListener(BiConsumer<KitType, Integer> listener)
	{
		kitChangeListeners.add(listener);
	}

	void addColorSwapListener(BiConsumer<ColorType, Integer> listener)
	{
		colorChangeListeners.add(listener);
	}

	void addLockListener(BiConsumer<KitType, Boolean> listener)
	{
		lockListeners.add(listener);
	}

	void removeListeners()
	{
		itemChangeListeners.clear();
		kitChangeListeners.clear();
		colorChangeListeners.clear();
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

	boolean containsItem(KitType slot)
	{
		return swappedItemIds.containsKey(slot);
	}

	boolean containsKit(KitType slot)
	{
		return swappedKitIds.containsKey(slot);
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

	Integer getColorOrDefault(ColorType type, Integer fallback)
	{
		return swappedColorIds.getOrDefault(type, fallback);
	}

	void putItem(KitType slot, Integer itemId)
	{
		if (lockedSlots.contains(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.put(slot, itemId);
		if (!itemId.equals(oldId))
		{
			itemChangeListeners.forEach(listener -> listener.accept(slot, itemId));
		}
	}

	void putKit(KitType slot, Integer kitId)
	{
		Integer oldId = swappedKitIds.put(slot, kitId);
		if (!kitId.equals(oldId))
		{
			kitChangeListeners.forEach(listener -> listener.accept(slot, kitId));
		}
	}

	void putColor(ColorType type, Integer colorId)
	{
		Integer oldId = swappedColorIds.put(type, colorId);
		if (!colorId.equals(oldId))
		{
			colorChangeListeners.forEach(listener -> listener.accept(type, colorId));
		}
	}

	void removeItem(KitType slot)
	{
		if (lockedSlots.contains(slot))
		{
			return;
		}
		Integer oldId = swappedItemIds.remove(slot);
		if (oldId != null)
		{
			itemChangeListeners.forEach(listener -> listener.accept(slot, null));
		}
	}

	void removeKit(KitType slot)
	{
		Integer oldId = swappedKitIds.remove(slot);
		if (oldId != null)
		{
			kitChangeListeners.forEach(listener -> listener.accept(slot, null));
		}
	}

	void removeColor(ColorType type)
	{
		Integer oldId = swappedColorIds.remove(type);
		if (oldId != null)
		{
			colorChangeListeners.forEach(listener -> listener.accept(type, null));
		}
	}

	void clear()
	{
		Set<KitType> slots = Sets.difference(new HashSet<>(swappedItemIds.keySet()), lockedSlots);
		for (KitType slot : slots)
		{
			removeItem(slot);
		}
		Set<KitType> kitSlots = new HashSet<>(swappedKitIds.keySet());
		for (KitType slot : kitSlots)
		{
			removeKit(slot);
		}
	}

	void replaceAllItems(Map<KitType, Integer> replacements)
	{
		// remove slot if in swaps but not in replacements
		Set<KitType> removes = new HashSet<>();
		swappedItemIds.forEach((slot, itemId) -> {
			if (!replacements.containsKey(slot))
			{
				removes.add(slot);
			}
		});
		removes.forEach(this::removeItem);
		// override slots in replacements
		replacements.forEach(this::putItem);
	}

	boolean isLocked(KitType slot)
	{
		return lockedSlots.contains(slot);
	}

	void toggleLocked(KitType slot)
	{
		for (KitType s : combinedLockSlotsFor(slot))
		{
			if (lockedSlots.contains(s))
			{
				lockedSlots.remove(s);
			}
			else
			{
				lockedSlots.add(s);
			}
			lockListeners.forEach(listener -> listener.accept(s, isLocked(slot)));
		}
	}

	void removeLocks()
	{
		Set<KitType> clears = new HashSet<>(lockedSlots);
		lockedSlots.clear();
		lockListeners.forEach(listener -> clears.forEach(slot -> listener.accept(slot, false)));
	}

	void removeLock(KitType slot)
	{
		for (KitType s : combinedLockSlotsFor(slot))
		{
			lockedSlots.remove(s);
			lockListeners.forEach(listener -> listener.accept(s, false));
		}
	}

	// hair, jaw, and head should all be synced as they have interactions, likewise for arms and torso
	private List<KitType> combinedLockSlotsFor(KitType slot)
	{
		if (HEAD_SLOTS.contains(slot))
		{
			return HEAD_SLOTS;
		}
		else if (CHEST_SLOTS.contains(slot))
		{
			return CHEST_SLOTS;
		}
		else
		{
			return Collections.singletonList(slot);
		}
	}
}
