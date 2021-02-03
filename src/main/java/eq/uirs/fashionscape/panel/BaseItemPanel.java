package eq.uirs.fashionscape.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

abstract class BaseItemPanel extends JPanel
{
	private static final Dimension ICON_SIZE = new Dimension(32, 32);

	public final Integer itemId;

	protected final ItemManager itemManager;
	protected final List<JPanel> highlightPanels;
	protected final Color nonHighlightColor;
	protected final JLabel itemLabel;
	protected final JLabel itemIcon;

	BaseItemPanel(@Nullable Integer itemId, BufferedImage icon, ItemManager itemManager)
	{
		this.itemManager = itemManager;
		this.itemId = itemId;
		String itemName = itemNameFor(itemId);

		BorderLayout layout = new BorderLayout();
		setLayout(layout);
		setBorder(new EmptyBorder(0, 10, 0, 10));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		highlightPanels = new ArrayList<>();
		nonHighlightColor = getBackground();
		highlightPanels.add(this);

		setBorder(new EmptyBorder(5, 5, 5, 5));

		// Icon
		itemIcon = new JLabel();
		itemIcon.setPreferredSize(ICON_SIZE);
		setItemIcon(icon);
		add(itemIcon, BorderLayout.LINE_START);

		itemLabel = new JLabel();
		itemLabel.setForeground(Color.WHITE);
		itemLabel.setMaximumSize(new Dimension(0, 0));
		itemLabel.setPreferredSize(new Dimension(0, 0));
		itemLabel.setText(itemName);
		// subclasses will add the item label as needed
	}

	void setItemIcon(BufferedImage icon)
	{
		if (icon != null)
		{
			if (icon instanceof AsyncBufferedImage)
			{
				((AsyncBufferedImage) icon).addTo(itemIcon);
			}
			else
			{
				itemIcon.setIcon(new ImageIcon(icon));
			}
		}
	}

	String itemNameFor(Integer itemId)
	{
		if (itemId != null)
		{
			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			return itemComposition.getName();
		}
		else
		{
			return "Nothing";
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
