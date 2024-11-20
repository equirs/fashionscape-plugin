package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.kit.KitType;

/**
 * Contains all virtual equipment changes in a single operation.
 * It's not a full snapshot of the player info; it only contains adds/removes.
 * A single operation could be simple, like changing/removing individual items/kits,
 * or it could be complex, like copying an entire outfit from another player.
 * "out" fields are outgoing (removed) whereas "in" fields are incoming (added)
 */
@Data
@AllArgsConstructor
public class Diff
{
	Map<KitType, SlotInfo> outSlots;
	Map<KitType, SlotInfo> inSlots;
	Map<ColorType, Integer> outColors;
	Map<ColorType, Integer> inColors;
	JawIcon outIcon;
	JawIcon inIcon;

	public static Diff empty()
	{
		return new Diff(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), null, null);
	}

	public static Diff ofSlots(Map<KitType, SlotInfo> outSlots, Map<KitType, SlotInfo> inSlots)
	{
		return new Diff(outSlots, inSlots, new HashMap<>(), new HashMap<>(), null, null);
	}

	public static Diff ofColor(ColorType type, Integer outColor, Integer inColor)
	{
		if (Objects.equals(outColor, inColor))
		{
			return Diff.empty();
		}
		Map<ColorType, Integer> outColors = new HashMap<>();
		outColors.put(type, outColor);
		Map<ColorType, Integer> inColors = new HashMap<>();
		inColors.put(type, inColor);
		return new Diff(new HashMap<>(), new HashMap<>(), outColors, inColors, null, null);
	}

	public static Diff ofIcon(JawIcon outIcon, JawIcon inIcon)
	{
		return new Diff(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), outIcon, inIcon);
	}

	// In the event of a collision, first takes priority over second
	public static Diff merge(Diff first, Diff second)
	{
		Map<KitType, SlotInfo> outSlots = new HashMap<>(second.outSlots);
		outSlots.putAll(first.outSlots);
		Map<KitType, SlotInfo> inSlots = new HashMap<>(second.inSlots);
		inSlots.putAll(first.inSlots);
		Map<ColorType, Integer> outColors = new HashMap<>(second.outColors);
		outColors.putAll(first.outColors);
		Map<ColorType, Integer> inColors = new HashMap<>(second.inColors);
		inColors.putAll(first.inColors);
		JawIcon outIcon = first.outIcon != null ? first.outIcon : second.outIcon;
		JawIcon inIcon = first.inIcon != null ? first.inIcon : second.inIcon;
		return new Diff(outSlots, inSlots, outColors, inColors, outIcon, inIcon);
	}

	public boolean isEmpty()
	{
		return equals(Diff.empty());
	}
}
