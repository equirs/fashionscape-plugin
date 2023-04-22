package eq.uirs.fashionscape.data;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum HairColor implements Colorable
{
	DARK_BROWN(0, new Color(93, 65, 29)),
	WHITE(1, new Color(227, 224, 224)),
	LIGHT_GREY(2, new Color(134, 144, 124)),
	DARK_GREY(3, new Color(72, 68, 68)),
	APRICOT(4, new Color(217, 148, 78)),
	STRAW(5, new Color(214, 185, 100)),
	LIGHT_BROWN(6, new Color(166, 123, 60)),
	BROWN(7, new Color(124, 89, 56)),
	TURQUOISE(8, new Color(18, 156, 161)),
	GREEN(9, new Color(18, 161, 24)),
	GINGER(10, new Color(179, 80, 21)),
	MAGENTA(11, new Color(198, 24, 207)),
	BLACK(12, new Color(44, 42, 42)),
	GREY(13, new Color(121, 109, 96)),
	BEIGE(14, new Color(187, 186, 149)),
	PEACH(15, new Color(217, 145, 101)),
	LIGHT_BLUE(16, new Color(144, 173, 202)),
	ROYAL_BLUE(17, new Color(62, 64, 173)),
	PALE_PINK(18, new Color(181, 146, 145)),
	INTENSE_PINK(19, new Color(204, 101, 95)),
	MAROON(20, new Color(87, 18, 9)),
	LIGHT_GREEN(21, new Color(145, 202, 146)),
	DARK_GREEN(22, new Color(59, 88, 79)),
	PURPLE(23, new Color(136, 65, 178)),
	LIGHT_PURPLE(24, new Color(185, 141, 200)),
	NAVY_BLUE(25, new Color(36, 37, 67)),
	BRIGHT_RED(26, new Color(155, 13, 5)),
	YELLOW(27, new Color(248, 203, 18)),
	DARK_PURPLE(28, new Color(56, 2, 74)),
	AQUA(29, new Color(31, 155, 127));

	private static final HairColor[] sorted = Arrays.stream(values())
		.sorted(Comparator.comparing(HairColor::getColorId))
		.toArray(HairColor[]::new);

	public static HairColor fromId(int id)
	{
		if (id < 0 || id >= sorted.length)
		{
			return null;
		}
		return values()[id];
	}

	@Getter
	private final int colorId;

	@Getter
	private final Color color;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}

	@Override
	public int getColorId(ColorType type)
	{
		return this.colorId;
	}
}
