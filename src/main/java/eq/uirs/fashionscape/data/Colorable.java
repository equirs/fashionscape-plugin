package eq.uirs.fashionscape.data;

import java.awt.Color;

public interface Colorable
{
	String getDisplayName();

	Color getColor();

	int getColorId(ColorType type);
}
