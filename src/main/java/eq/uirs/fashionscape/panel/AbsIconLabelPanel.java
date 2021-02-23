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
	protected final JLabel label;
	protected final JLabel icon;

	AbsIconLabelPanel(BufferedImage image, ClientThread clientThread)
	{
		this.clientThread = clientThread;

		BorderLayout layout = new BorderLayout();
		setLayout(layout);
		setBorder(new EmptyBorder(0, 10, 0, 10));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		highlightPanels = new ArrayList<>();
		nonHighlightColor = getBackground();
		highlightPanels.add(this);

		setBorder(new EmptyBorder(5, 5, 5, 5));

		icon = new JLabel();
		icon.setPreferredSize(ICON_SIZE);
		setIcon(image);
		add(icon, BorderLayout.LINE_START);

		label = new JLabel();
		label.setForeground(Color.WHITE);
		label.setMaximumSize(new Dimension(0, 0));
		label.setPreferredSize(new Dimension(0, 0));
		// subclasses will add the item label as needed
	}

	protected void setIcon(BufferedImage image)
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

	protected void matchComponentBackground(JPanel panel, Color color)
	{
		panel.setBackground(color);
		for (Component c : panel.getComponents())
		{
			c.setBackground(color);
		}
	}
}
