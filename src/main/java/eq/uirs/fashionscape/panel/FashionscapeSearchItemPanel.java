package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

public class FashionscapeSearchItemPanel extends BaseItemPanel
{
	private final SwapManager swapManager;
	private final KitType slot;

	FashionscapeSearchItemPanel(@Nullable Integer itemId, AsyncBufferedImage icon, KitType slot,
								ItemManager itemManager, SwapManager swapManager, ClientThread clientThread,
								OnSelectionChangingListener listener, Double score)
	{
		super(itemId, icon, itemManager, clientThread);
		this.swapManager = swapManager;
		this.slot = slot;

		MouseAdapter itemPanelMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!swapManager.isLocked(slot))
				{
					for (JPanel panel : highlightPanels)
					{
						matchComponentBackground(panel, ColorScheme.DARK_GRAY_HOVER_COLOR);
					}
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				swapManager.hoverOver(itemId);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, defaultBackgroundColor());
				}
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				swapManager.hoverAway();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				// pre-emptively set background
				boolean isAlreadySelected = Objects.equals(swapManager.swappedItemIdIn(slot), itemId);
				Color bg = isAlreadySelected ? nonHighlightColor : ColorScheme.MEDIUM_GRAY_COLOR;
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, bg);
				}
				// now swap it
				clientThread.invokeLater(() -> {
					if (!swapManager.isLocked(itemId))
					{
						listener.onSearchSelectionChanging(slot);
						swapManager.hoverSelect(itemId);
					}
				});
			}
		};

		addMouseListener(itemPanelMouseListener);

		// Item details panel
		JPanel rightPanel = new JPanel(new GridLayout(2, 1));
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
		highlightPanels.add(rightPanel);
		rightPanel.add(itemLabel);

		JLabel scoreLabel = new JLabel();
		scoreLabel.setForeground(Color.WHITE);
		scoreLabel.setMaximumSize(new Dimension(0, 0));
		scoreLabel.setPreferredSize(new Dimension(0, 0));
		if (score != null)
		{
			scoreLabel.setText(score.toString());
		}
		rightPanel.add(scoreLabel);

		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, defaultBackgroundColor());
		}

		add(rightPanel, BorderLayout.CENTER);
	}

	public void resetBackground()
	{
		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, nonHighlightColor);
		}
	}

	private Color defaultBackgroundColor()
	{
		if (Objects.equals(swapManager.swappedItemIdIn(slot), itemId))
		{
			return ColorScheme.MEDIUM_GRAY_COLOR;
		}
		else
		{
			return nonHighlightColor;
		}
	}
}
