package eq.uirs.fashionscape.core.model;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import net.runelite.api.kit.KitType;

/**
 * Observable wrapper around fashionscape items.
 * Kit ids are stored separately (in Kits class) because of potential overlap.
 * SlotInfo.nothing can be stored here.
 */
@Data
@Getter(AccessLevel.PRIVATE)
public class Items
{
	private final ModelType modelType;
	private final Map<KitType, SlotInfo> value = new HashMap<>();

	public ImmutableMap<KitType, SlotInfo> getAll()
	{
		return ImmutableMap.copyOf(value);
	}

	@Nullable
	public SlotInfo get(KitType slot)
	{
		return value.get(slot);
	}

	public boolean containsKey(KitType slot)
	{
		return get(slot) != null;
	}

	public SlotInfo remove(KitType slot)
	{
		return put(slot, null);
	}

	@Nullable
	public SlotInfo put(KitType slot, @Nullable SlotInfo info)
	{
		SlotInfo oldInfo = info == null ? value.remove(slot) : value.put(slot, info);
		Events.fire(new ItemChanged(slot, modelType, info));
		return oldInfo;
	}

	public void putAll(Map<KitType, SlotInfo> other)
	{
		other.forEach(this::put);
	}

	public void clear()
	{
		Set<KitType> removes = new HashSet<>(value.keySet());
		removes.forEach(this::remove);
	}
}
