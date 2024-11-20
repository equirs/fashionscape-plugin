package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Represents an item in search results
 */
@Slf4j
class SearchItemPanel extends AbsItemPanel
{
	public final int itemId;

	private final FashionManager fashionManager;
	private final KitType slot;
	private final SlotInfo slotInfo;

	public SearchItemPanel(int itemId, BufferedImage icon, KitType slot,
						   ItemManager itemManager, FashionManager fashionManager, ClientThread clientThread,
						   OnSelectionChangingListener listener, Double score, boolean developerMode)
	{
		super(icon, itemManager, clientThread, developerMode);
		this.itemId = itemId;
		this.fashionManager = fashionManager;
		this.slot = slot;
		this.slotInfo = SlotInfo.lookUp(itemId + FashionManager.ITEM_OFFSET, slot);

		MouseAdapter itemPanelMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!fashionManager.getLocks().isAllowed(slot, slotInfo))
				{
					return;
				}
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, ColorScheme.DARK_GRAY_HOVER_COLOR);
				}
				setCursor(new Cursor(Cursor.HAND_CURSOR));
				fashionManager.hoverOverItem(slot, itemId);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, defaultBackgroundColor());
				}
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				fashionManager.hoverAway();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (!fashionManager.getLocks().isAllowed(slot, slotInfo))
				{
					return;
				}
				// pre-emptively set background
				boolean isAlreadySelected = isMatch();
				Color bg = isAlreadySelected ? nonHighlightColor : ColorScheme.MEDIUM_GRAY_COLOR;
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, bg);
				}
				// now set it
				clientThread.invokeLater(() -> {
					listener.onSearchSelectionChanging(slot);
					fashionManager.hoverSelectItem(slot, itemId);
				});
			}
		};

		addMouseListener(itemPanelMouseListener);

		// Item details panel
		int rows = score == null ? 1 : 2;
		JPanel rightPanel = new JPanel(new GridLayout(rows, 1));
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
		highlightPanels.add(rightPanel);
		rightPanel.add(label);

		if (score != null)
		{
			DecimalFormat format = new DecimalFormat("#.#");
			JLabel scoreLabel = new JLabel();
			scoreLabel.setForeground(Color.WHITE);
			scoreLabel.setMaximumSize(new Dimension(0, 0));
			scoreLabel.setPreferredSize(new Dimension(0, 0));
			scoreLabel.setText(format.format(score * 100.0) + "%");
			scoreLabel.setForeground(getScoreColor(score));
			rightPanel.add(scoreLabel);
		}

		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, defaultBackgroundColor());
		}

		setItemName(itemId);
		add(rightPanel, BorderLayout.CENTER);
		checkLockTooltip();
	}

	private void checkLockTooltip()
	{
		Set<KitType> conflicts = fashionManager.getLocks().conflictingSlots(slot, slotInfo);
		if (conflicts.isEmpty())
		{
			setToolTipText(null);
			label.setForeground(Color.WHITE);
		}
		else
		{
			String conflictString = conflicts.stream().sorted()
				.map(KitType::name)
				.map(String::toLowerCase)
				.collect(Collectors.joining(", "));
			setToolTipText("some locked slots prevent this change: " + conflictString);
			label.setForeground(Color.GRAY);
		}
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
		if (isMatch())
		{
			return ColorScheme.MEDIUM_GRAY_COLOR;
		}
		else
		{
			return nonHighlightColor;
		}
	}

	private boolean isMatch()
	{
		if (itemId < 0)
		{
			return fashionManager.isNothing(slot);
		}
		else
		{
			return Objects.equals(itemId, fashionManager.virtualItemIdFor(slot));
		}
	}

	private Color getScoreColor(Double score)
	{
		int red = (int) (255.0 * (1 - Math.pow(score, 2)));
		int green = (int) (255.0 * Math.pow(score, 2));
		return new Color(red, green, 0);
	}
}
