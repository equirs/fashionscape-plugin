package eq.uirs.fashionscape.core.layer;

import com.google.common.collect.Sets;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.LockStatus;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.event.ColorLockChanged;
import eq.uirs.fashionscape.core.event.IconLockChanged;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.model.Items;
import eq.uirs.fashionscape.data.color.ColorType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.NotNull;

/**
 * Stores info about item+kit slots, color ids, and jaw icons that the user does not want to change.
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Locks
{
	private final Layers layers;

	private final Map<KitType, LockStatus> slots = new HashMap<>();
	private final Set<ColorType> colors = new HashSet<>();
	@Getter
	private boolean icon = false;

	@Nullable
	public LockStatus get(KitType slot)
	{
		return slots.get(slot);
	}

	/**
	 * Whether the slot is locked on a non-null LockStatus.
	 * Use `get` if the exact status is needed.
	 */
	public boolean contains(KitType slot)
	{
		return get(slot) != null;
	}

	/**
	 * Whether the color for a player model section is locked
	 */
	public boolean getColor(ColorType type)
	{
		return colors.contains(type);
	}

	public void set(KitType slot, @Nullable LockStatus value)
	{
		if (value == null)
		{
			slots.remove(slot);
		}
		else
		{
			slots.put(slot, value);
		}
		Events.fire(new LockChanged(slot, value));
	}

	public void set(ColorType color, boolean locked)
	{
		if (locked)
		{
			colors.add(color);
		}
		else
		{
			colors.remove(color);
		}
		Events.fire(new ColorLockChanged(color, locked));
	}

	public void setIcon(boolean locked)
	{
		icon = locked;
		Events.fire(new IconLockChanged(locked));
	}

	public void toggle(KitType slot, @NonNull LockStatus type)
	{
		LockStatus old = slots.get(slot);
		// if slot wasn't locked (or going from item -> kit+item), add to locks. otherwise unlock
		if (old == null || (old == LockStatus.ITEM && type == LockStatus.ALL))
		{
			set(slot, type);
		}
		else
		{
			set(slot, null);
		}
	}

	public void toggle(ColorType color)
	{
		set(color, !getColor(color));
	}

	public void toggleIcon()
	{
		setIcon(!icon);
	}

	public void clear()
	{
		new HashSet<>(slots.keySet()).forEach(k -> set(k, null));
		new HashSet<>(colors).forEach(c -> set(c, false));
		setIcon(false);
	}

	public boolean isAllowed(KitType slot, @Nullable SlotInfo info)
	{
		return conflictingSlots(slot, info).isEmpty();
	}

	/**
	 * Produces a set of slots which are in "conflict" with the arguments, i.e., the current state of the user's
	 * locks prevent this change from happening. If no conflicts exist, returns an empty set.
	 */
	@NotNull
	public Set<KitType> conflictingSlots(KitType slot, @Nullable SlotInfo info)
	{
		Set<KitType> conflicts = new HashSet<>();
		Items items = layers.getVirtualModels().getItems();
		SlotInfo existing = items.get(slot);
		// 1. is the slot locked?
		LockStatus status = slots.get(slot);
		if (status != null)
		{
			// lock will prevent if this change involves items or if both kits+items are locked
			boolean changingItems = existing != null && existing.isItem() || info != null && info.isItem();
			if (status == LockStatus.ALL || changingItems)
			{
				conflicts.add(slot);
			}
		}
		// 2. could a locked item in another slot be forcibly hiding this slot, preventing any change?
		// e.g., if locked on a full helm item, can't change hair/jaw
		// note that placing "nothing" in this example would still not be allowable
		items.getAll().values().stream()
			.filter(s -> s.getSlot() != slot && contains(s.getSlot()) && s.hides(slot))
			.map(SlotInfo::getSlot)
			.forEach(conflicts::add);
		// 3. will this change hide or un-hide any other locked slots as a side effect?
		// e.g., if hair is locked, any head slot change must keep hair shown/hidden as it was before.
		// if multiple items are removed by a single slot change, before/after must still match on hidden+locked slots.
		// these cases are rare but possible with items like hooded cloak (from cw), a cape that hides helm+hair
		Set<KitType> locked = slots.keySet();
		Set<KitType> incHidden = info != null ? info.getHidden() : new HashSet<>();
		Set<KitType> outHidden = existing != null ? existing.getHidden() : new HashSet<>();
		Set<KitType> hiddenLockedIncoming = new HashSet<>(Sets.intersection(incHidden, locked));
		Set<KitType> hiddenLockedOutgoing = new HashSet<>(Sets.intersection(outHidden, locked));
		// find all unlocked items which the incoming item hides
		List<SlotInfo> outIncHides = incHidden.stream()
			.filter(s -> !contains(s) && items.containsKey(s))
			.map(items::get)
			.collect(Collectors.toList());
		// find all unlocked items that either hide incoming slot or share hidden slots with incoming item
		// note: sharing hidden slots is conflicting because these items often visually occupy the slots they hide
		List<SlotInfo> outHidesInc = items.getAll().values().stream()
			.filter(i -> !contains(i.getSlot()) &&
				(i.hides(slot) || !Sets.intersection(i.getHidden(), incHidden).isEmpty()))
			.collect(Collectors.toList());
		// all slots hidden+locked by these items are added to "outgoing" set
		Stream.concat(outIncHides.stream(), outHidesInc.stream())
			.flatMap(i -> Sets.intersection(i.getHidden(), locked).stream())
			.forEach(hiddenLockedOutgoing::add);
		// any slot in one set but not the other is considered to be in conflict
		conflicts.addAll(Sets.symmetricDifference(hiddenLockedIncoming, hiddenLockedOutgoing));
		return conflicts;
	}

	// restores previously-set locks from config
	public void restore(Map<KitType, LockStatus> slots, Map<ColorType, Boolean> colors, boolean icon)
	{
		for (KitType slot : KitType.values())
		{
			set(slot, slots.get(slot));
		}
		for (ColorType type : ColorType.values())
		{
			set(type, Objects.equals(true, colors.get(type)));
		}
		setIcon(icon);
	}
}
