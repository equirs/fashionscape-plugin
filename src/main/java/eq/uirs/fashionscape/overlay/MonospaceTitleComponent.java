package eq.uirs.fashionscape.overlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

@Setter
@Builder
public class MonospaceTitleComponent implements LayoutableRenderableEntity
{
	private String text;

	@Builder.Default
	private Color color = Color.WHITE;

	@Builder.Default
	private Point preferredLocation = new Point();

	@Builder.Default
	private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);

	@Builder.Default
	@Getter
	private final Rectangle bounds = new Rectangle();

	@Builder.Default
	private Font font = new Font("Monospaced", Font.PLAIN, 9);

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final int baseX = preferredLocation.x;
		final int baseY = preferredLocation.y;
		final FontMetrics metrics = graphics.getFontMetrics(font);
		final TextComponent titleComponent = new TextComponent();
		titleComponent.setText(text);
		titleComponent.setColor(color);
		titleComponent.setFont(font);
		titleComponent.setPosition(new Point(baseX, baseY + metrics.getHeight()));
		final Dimension rendered = titleComponent.render(graphics);
		final Dimension dimension = new Dimension(preferredSize.width, rendered.height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}
}
