package eq.uirs.fashionscape.data;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum SkinColor implements Colorable
{
	VERY_PALE(7, new Color(213, 169, 133)),
	PALE(0, new Color(187, 143, 106)),
	NORMAL(1, new Color(164, 127, 93)),
	SLIGHTLY_TAN(2, new Color(158, 122, 73)),
	TAN(3, new Color(131, 110, 66)),
	VERY_TAN(4, new Color(121, 88, 42)),
	DARK(5, new Color(100, 78, 36)),
	VERY_DARK(6, new Color(76, 61, 9)),
	GREEN(8, new Color(0, 127, 0)),
	BLACK(9, new Color(15, 15, 15)),
	WHITE(10, new Color(255, 255, 255)),
	CYAN(11, new Color(5, 152, 159)),
	PURPLE(12, new Color(100, 65, 164));

	@Getter
	private final int colorId;

	@Getter
	private final Color color;

	@Override
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
