package eq.uirs.fashionscape.core.model;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.event.ColorChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.color.BootsColor;
import eq.uirs.fashionscape.data.color.ClothingColor;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import eq.uirs.fashionscape.data.color.HairColor;
import eq.uirs.fashionscape.data.color.SkinColor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Observable wrapper around recolor-able models.
 */
@Data
@Setter(AccessLevel.PRIVATE)
public class Colors
{
	private final ModelType modelType;
	// values are color ids, not rgb ints; see Colorable interface
	private final Map<ColorType, Integer> value = new HashMap<>();

	public ImmutableMap<ColorType, Integer> getAll()
	{
		return ImmutableMap.copyOf(value);
	}

	public Map<ColorType, Colorable> getAllColorable()
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
			.filter(value::containsKey)
			.collect(Collectors.toMap(t -> t, t -> getColor.apply(t, get(t))));
	}

	public Integer get(ColorType slot)
	{
		return value.get(slot);
	}

	public Integer remove(ColorType slot)
	{
		return put(slot, null);
	}

	public Integer put(ColorType slot, Integer colorId)
	{
		Integer oldId = colorId == null ? value.remove(slot) : value.put(slot, colorId);
		Events.fire(new ColorChanged(slot, modelType, colorId));
		return oldId;
	}

	public void putAll(Map<ColorType, Integer> other)
	{
		other.forEach(this::put);
	}

	public void clear()
	{
		Set<ColorType> removes = new HashSet<>(value.keySet());
		removes.forEach(this::remove);
	}
}
