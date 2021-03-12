package eq.uirs.fashionscape.data;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum ClothingColor implements Colorable
{
	KHAKI(0, 1, new Color(123, 110, 50)),
	CHARCOAL(1, 2, new Color(38, 35, 35)),
	CRIMSON(2, 3, new Color(117, 47, 59)),
	NAVY(3, 4, new Color(59, 59, 78)),
	STRAW(4, 5, new Color(170, 144, 69)),
	WHITE(5, 6, new Color(173, 164, 129)),
	RED(6, 7, new Color(139, 59, 42)),
	BLUE(7, 8, new Color(45, 82, 114)),
	GREEN(8, 9, new Color(45, 114, 62)),
	YELLOW(9, 10, new Color(163, 151, 13)),
	PURPLE(10, 11, new Color(131, 83, 123)),
	ORANGE(11, 12, new Color(157, 100, 47)),
	ROSE(12, 13, new Color(161, 112, 103)),
	LIME(13, 14, new Color(130, 148, 114)),
	CYAN(14, 15, new Color(114, 140, 148)),
	EMERALD(15, 0, new Color(28, 97, 53)),
	BLACK(16, 16, new Color(16, 16, 16)),
	GREY(17, 17, new Color(64, 64, 64)),
	ONION(18, 18, new Color(248, 216, 144)),
	PEACH(19, 19, new Color(248, 190, 130)),
	LUMBRIDGE_BLUE(20, 20, new Color(150, 150, 255)),
	DEEP_BLUE(21, 21, new Color(0, 0, 255)),
	LIGHT_PINK(22, 22, new Color(255, 190, 190)),
	CADMIUM_RED(23, 23, new Color(200, 80, 50)),
	MAROON(24, 24, new Color(150, 40, 0)),
	PALE_GREEN(25, 25, new Color(200, 255, 200)),
	TURQUOISE(26, 26, new Color(0, 120, 120)),
	DEEP_PURPLE(27, 27, new Color(150, 0, 150)),
	LIGHT_PURPLE(28, 28, new Color(255, 150, 255));

	private static final ClothingColor[] torsoSorted = Arrays.stream(values())
		.sorted(Comparator.comparing(ClothingColor::getTorsoColorId))
		.toArray(ClothingColor[]::new);

	private static final ClothingColor[] legsSorted = Arrays.stream(values())
		.sorted(Comparator.comparing(ClothingColor::getLegsColorId))
		.toArray(ClothingColor[]::new);

	public static ClothingColor fromTorsoId(int id)
	{
		if (id < 0 || id >= torsoSorted.length)
		{
			return null;
		}
		return values()[id];
	}

	public static ClothingColor fromLegsId(int id)
	{
		if (id < 0 || id >= legsSorted.length)
		{
			return null;
		}
		return values()[id];
	}

	@Getter
	private final int torsoColorId;

	@Getter
	private final int legsColorId;

	@Getter
	private final Color color;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}

	@Override
	public int getColorId(ColorType type)
	{
		if (type == ColorType.TORSO)
		{
			return torsoColorId;
		}
		else
		{
			return legsColorId;
		}
	}
}
