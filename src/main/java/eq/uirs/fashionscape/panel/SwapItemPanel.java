package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.SwapManager;
import eq.uirs.fashionscape.core.event.ItemChangedListener;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j
class SwapItemPanel extends AbsItemPanel
{
	private static final Dimension ICON_SIZE = new Dimension(20, 20);

	private final KitType slot;
	private final SwapManager swapManager;
	private final JButton lockButton;
	private final JButton xButton;
	private final SearchOpener searchOpener;
	private Integer itemId;
	private MouseAdapter mouseAdapter = null;
	private MouseAdapter hoverAdapter = null;

	public SwapItemPanel(@Nullable Integer itemId, BufferedImage icon, ItemManager itemManager,
						 ClientThread clientThread, SwapManager swapManager, KitType slot,
						 SearchOpener searchOpener)
	{
		super(itemId, icon, itemManager, clientThread);
		this.slot = slot;
		this.itemId = itemId;
		this.swapManager = swapManager;
		this.searchOpener = searchOpener;
		// Item details panel
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		lockButton = new JButton();
		lockButton.setBorder(new EmptyBorder(0, 2, 0, 2));
		lockButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		lockButton.setPreferredSize(ICON_SIZE);
		lockButton.setFocusPainted(false);
		lockButton.setBorderPainted(false);
		lockButton.setContentAreaFilled(false);
		lockButton.addActionListener(e -> {
			swapManager.toggleItemLocked(slot);
			updateLockButton();
			updateXButton();
		});
		buttons.add(lockButton);

		xButton = new JButton();
		xButton.setPreferredSize(ICON_SIZE);
		xButton.setBorder(new EmptyBorder(0, 2, 0, 2));
		xButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		xButton.setFocusPainted(false);
		xButton.setBorderPainted(false);
		xButton.setContentAreaFilled(false);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revertSlot(slot);
			SwingUtilities.invokeLater(() -> {
				updateLockButton();
				updateXButton();
			});
		}));
		buttons.add(xButton);

		updateLockButton();
		updateXButton();

		rightPanel.add(buttons, BorderLayout.EAST);

		add(rightPanel, BorderLayout.CENTER);

		swapManager.addEventListener(new ItemChangedListener(e -> {
			if (e.getSlot() == slot)
			{
				Integer newId = e.getItemId();
				this.itemId = newId;
				setItemName(newId);
				setItemIcon(newId);
				resetMouseListeners();
				updateXButton();
			}
		}));

		swapManager.addEventListener(new LockChangedListener(e -> {
			if (e.getSlot() == slot && e.getType() != LockChanged.Type.KIT)
			{
				updateLockButton();
			}
		}));

		resetMouseListeners();
	}

	private void resetMouseListeners()
	{
		// Somehow these listeners clear when the item changes, so they're refreshed like this (ugh)
		icon.removeMouseListener(mouseAdapter);
		mouseAdapter = createOpenSearchClickListener();
		icon.addMouseListener(mouseAdapter);
		icon.setToolTipText("Open " + slot.name().toLowerCase() + " slot search");

		icon.removeMouseListener(hoverAdapter);
		xButton.removeMouseListener(hoverAdapter);
		lockButton.removeMouseListener(hoverAdapter);
		hoverAdapter = createHoverListener();
		icon.addMouseListener(hoverAdapter);
		xButton.addMouseListener(hoverAdapter);
		lockButton.addMouseListener(hoverAdapter);
	}

	void updateLockButton()
	{
		boolean locked = swapManager.isItemLocked(slot);
		String lockIcon = locked ? "lock" : "unlock";
		lockButton.setIcon(
			new ImageIcon(ImageUtil.loadImageResource(this.getClass(), lockIcon + ".png")));
		String action = locked ? "Unlock" : "Lock";
		lockButton.setToolTipText(action + " " + slot.name().toLowerCase() + " slot");
	}

	void updateXButton()
	{
		xButton.setEnabled(itemId != null);
		xButton.setToolTipText("Clear " + slot.name().toLowerCase() + " slot");
	}

	private MouseAdapter createOpenSearchClickListener()
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				searchOpener.openSearchFor(slot);
			}
		};
	}

	private MouseAdapter createHoverListener()
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (e.getComponent().isEnabled())
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		};
	}

	private void setItemIcon(Integer itemId)
	{
		if (itemId != null && itemId >= 0)
		{
			clientThread.invokeLater(() -> {
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				AsyncBufferedImage image = itemManager.getImage(itemComposition.getId());
				image.addTo(icon);
			});
		}
		else
		{
			BufferedImage image = ImageUtil.loadImageResource(getClass(), slot.name().toLowerCase() + ".png");
			setIcon(icon, image);
		}
	}

}
