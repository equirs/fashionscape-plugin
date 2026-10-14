package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.panel.AbsItemPanel;
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
import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

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

	public SearchItemPanel(SearchResult result, BufferedImage icon, ItemManager itemManager,
						   FashionManager fashionManager, ClientThread clientThread,
						   OnSelectionChangingListener listener, boolean developerMode)
	{
		super(icon, itemManager, clientThread, developerMode);
		int itemId = result.getId();
		KitType slot = result.getSlot();
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
				if (!SwingUtilities.isLeftMouseButton(e) || !fashionManager.getLocks().isAllowed(slot, slotInfo))
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
		setWikiMenu(itemId);

		// Item details panel
		JLabel detailLabel = createDetailLabel(result);
		JPanel rightPanel = new JPanel(new GridLayout(detailLabel == null ? 1 : 2, 1));
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
		highlightPanels.add(rightPanel);
		rightPanel.setInheritsPopupMenu(true);
		rightPanel.add(label);
		if (detailLabel != null)
		{
			rightPanel.add(detailLabel);
		}

		setItemName(itemId);
		add(rightPanel, BorderLayout.CENTER);
		refreshState();
	}

	/**
	 * Updates the selection highlight and lock tooltip to match the current outfit
	 */
	void refreshState()
	{
		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, defaultBackgroundColor());
		}
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
		return isMatch() ? ColorScheme.MEDIUM_GRAY_COLOR : nonHighlightColor;
	}

	private boolean isMatch()
	{
		return itemId < 0 ? fashionManager.isNothing(slot) : Objects.equals(itemId, fashionManager.virtualItemIdFor(slot));
	}

	// shows the color match score or GE price, depending on the sort
	@Nullable
	private static JLabel createDetailLabel(SearchResult result)
	{
		JLabel detail = new JLabel();
		detail.setMaximumSize(new Dimension(0, 0));
		detail.setPreferredSize(new Dimension(0, 0));
		if (result.getScore() != null)
		{
			detail.setText(new DecimalFormat("#.#").format(result.getScore() * 100.0) + "%");
			detail.setForeground(getScoreColor(result.getScore()));
		}
		else if (result.getPrice() != null)
		{
			long price = result.getPrice();
			detail.setText(price > 0 ? QuantityFormatter.quantityToStackSize(price) + " gp" : "No price");
			detail.setForeground(price > 0 ? ColorScheme.GRAND_EXCHANGE_PRICE : ColorScheme.LIGHT_GRAY_COLOR);
		}
		else
		{
			return null;
		}
		return detail;
	}

	private static Color getScoreColor(Double score)
	{
		int red = (int) (255.0 * (1 - Math.pow(score, 2)));
		int green = (int) (255.0 * Math.pow(score, 2));
		return new Color(red, green, 0);
	}
}
