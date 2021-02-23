package eq.uirs.fashionscape.data;

import java.awt.Color;
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
