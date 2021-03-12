package eq.uirs.fashionscape.data;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum BootsColor implements Colorable
{
	BROWN(0, new Color(111, 89, 61)),
	KHAKI(1, new Color(88, 88, 8)),
	ASHEN(2, new Color(121, 113, 96)),
	DARK(3, new Color(65, 60, 60)),
	TERRACOTTA(4, new Color(129, 98, 58)),
	GREY(5, new Color(115, 114, 106));

	private static final BootsColor[] sorted = Arrays.stream(values())
		.sorted(Comparator.comparing(BootsColor::getColorId))
		.toArray(BootsColor[]::new);

	public static BootsColor fromId(int id)
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
