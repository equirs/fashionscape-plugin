package eq.uirs.fashionscape.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

abstract class AbsIconLabelPanel extends JPanel
{
	static final Dimension ICON_SIZE = new Dimension(32, 32);

	protected final ClientThread clientThread;
	protected final List<JPanel> highlightPanels;
	protected final Color nonHighlightColor;
	protected final AutoTooltipLabel label;
	protected final JLabel icon;

	AbsIconLabelPanel(BufferedImage image, ClientThread clientThread)
	{
		this.clientThread = clientThread;

		BorderLayout layout = new BorderLayout();
		setLayout(layout);
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		highlightPanels = new ArrayList<>();
		nonHighlightColor = getBackground();
		highlightPanels.add(this);

		icon = new JLabel();
		icon.setPreferredSize(ICON_SIZE);
		icon.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setIcon(icon, image);
		add(icon, BorderLayout.LINE_START);

		label = new AutoTooltipLabel();
		label.setForeground(Color.WHITE);
		label.setMaximumSize(new Dimension(0, 0));
		label.setPreferredSize(new Dimension(0, 0));
		// subclasses will add the item label as needed
	}

	protected void setIcon(JLabel icon, BufferedImage image)
	{
		if (image != null)
		{
			if (image instanceof AsyncBufferedImage)
			{
				((AsyncBufferedImage) image).addTo(icon);
			}
			else
			{
				icon.setIcon(new ImageIcon(image));
			}
		}
	}

	public void resetBackground()
	{
		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, nonHighlightColor);
		}
	}

	protected void matchComponentBackground(JPanel panel, Color color)
	{
		panel.setBackground(color);
		for (Component c : panel.getComponents())
		{
			c.setBackground(color);
		}
	}

	protected Color getScoreColor(Double score)
	{
		int red = (int) (255.0 * (1 - Math.pow(score, 2)));
		int green = (int) (255.0 * Math.pow(score, 2));
		return new Color(red, green, 0);
	}
}
