package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Splits width evenly between loadout colors. Draws each distinct color once.
 */
class LoadoutColorBar extends JComponent
{
	private static final int HEIGHT = 4;

	private final List<Color> colors;

	LoadoutColorBar(Loadout loadout)
	{
		Set<Color> distinct = new LinkedHashSet<>();
		for (ColorType type : ColorType.values())
		{
			Integer colorId = loadout.getColors().get(type);
			if (colorId != null)
			{
				Arrays.stream(type.getColorables())
					.filter(c -> c.getColorId(type) == colorId)
					.findFirst()
					.map(Colorable::getColor)
					.ifPresent(distinct::add);
			}
		}
		colors = new ArrayList<>(distinct);
		setPreferredSize(new Dimension(0, colors.isEmpty() ? 0 : HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		int width = getWidth();
		int n = colors.size();
		for (int i = 0; i < n; i++)
		{
			// compute both edges from the total width (prevent gaps from rounding)
			int left = width * i / n;
			int right = width * (i + 1) / n;
			g.setColor(colors.get(i));
			g.fillRect(left, 0, right - left, getHeight());
		}
	}
}
