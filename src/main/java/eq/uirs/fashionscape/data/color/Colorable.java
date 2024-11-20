package eq.uirs.fashionscape.data.color;

import java.awt.Color;

public interface Colorable
{
	String getDisplayName();

	Color getColor();

	int getColorId(ColorType type);
}
