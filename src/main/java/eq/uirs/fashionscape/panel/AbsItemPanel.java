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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

public abstract class AbsItemPanel extends JPanel
{
	private static final Dimension ICON_SIZE = new Dimension(32, 32);

	public final Integer itemId;

	protected final ItemManager itemManager;
	protected final ClientThread clientThread;
	protected final List<JPanel> highlightPanels;
	protected final Color nonHighlightColor;
	protected final JLabel itemLabel;
	protected final JLabel itemIcon;

	AbsItemPanel(@Nullable Integer itemId, BufferedImage icon, ItemManager itemManager, ClientThread clientThread)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.itemId = itemId;

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
		setItemName(itemId);
		// subclasses will add the item label as needed
	}

	protected void setItemIcon(BufferedImage icon)
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

	protected void setItemName(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			String itemName = "Nothing";
			if (itemId != null)
			{
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				itemName = itemComposition.getName();
			}
			itemLabel.setText(itemName);
		});
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
