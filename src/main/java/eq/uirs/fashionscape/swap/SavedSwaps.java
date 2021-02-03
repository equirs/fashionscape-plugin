package eq.uirs.fashionscape.swap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;

// observable wrapper for user's item swaps
@RequiredArgsConstructor
@Slf4j
class SavedSwaps
{
	private final Map<KitType, Integer> swappedItemIds = new HashMap<>();
	private final Set<KitType> lockedSlots = new HashSet<>();
	private final List<BiConsumer<KitType, Integer>> itemChangeListeners = new LinkedList<>();
	private final List<BiConsumer<KitType, Boolean>> lockListeners = new LinkedList<>();

	void addSwapListener(BiConsumer<KitType, Integer> listener)
	{
		itemChangeListeners.add(listener);
	}

	void addLockListener(BiConsumer<KitType, Boolean> listener)
	{
		lockListeners.add(listener);
	}

	void removeListeners()
	{
		itemChangeListeners.clear();
	}

	Set<Map.Entry<KitType, Integer>> entrySet()
	{
		return ImmutableSet.copyOf(swappedItemIds.entrySet());
	}

	Integer get(KitType slot)
	{
		return swappedItemIds.get(slot);
	}

	Integer getOrDefault(KitType slot, Integer fallback)
	{
		return swappedItemIds.getOrDefault(slot, fallback);
	}

	void put(KitType slot, Integer itemId)
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

	void remove(KitType slot)
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

	void clear()
	{
		Set<KitType> slots = Sets.difference(new HashSet<>(swappedItemIds.keySet()), lockedSlots);
		for (KitType slot : slots)
		{
			remove(slot);
		}
	}

	void replaceAll(Map<KitType, Integer> replacements)
	{
		// remove slot if in swaps but not in replacements
		Set<KitType> removes = new HashSet<>();
		swappedItemIds.forEach((slot, itemId) -> {
			if (!replacements.containsKey(slot))
			{
				removes.add(slot);
			}
		});
		removes.forEach(this::remove);
		// override slots in replacements
		replacements.forEach(this::put);
	}

	boolean isLocked(KitType slot)
	{
		return lockedSlots.contains(slot);
	}

	void toggleLocked(KitType slot)
	{
		if (lockedSlots.contains(slot))
		{
			lockedSlots.remove(slot);
		}
		else
		{
			lockedSlots.add(slot);
		}
		lockListeners.forEach(listener -> listener.accept(slot, isLocked(slot)));
	}

	void removeLocks()
	{
		Set<KitType> clears = new HashSet<>(lockedSlots);
		lockedSlots.clear();
		lockListeners.forEach(listener -> clears.forEach(slot -> listener.accept(slot, false)));
	}

}
