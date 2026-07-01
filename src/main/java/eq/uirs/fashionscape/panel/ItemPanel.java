package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Panel for each item slot, showing virtual item, nothing, or unset
 */
@Slf4j
class ItemPanel extends AbsItemPanel
{
	private static final Dimension ICON_SIZE = new Dimension(20, 20);

	private final KitType slot;
	private final FashionManager fashionManager;
	private final JButton lockButton;
	private final JButton xButton;
	private final SearchOpener searchOpener;
	private MouseAdapter mouseAdapter = null;
	private MouseAdapter hoverAdapter = null;

	public ItemPanel(BufferedImage image, ItemManager itemManager,
	                 ClientThread clientThread, FashionManager fashionManager, KitType slot,
	                 SearchOpener searchOpener, boolean developerMode)
	{
		super(image, itemManager, clientThread, developerMode);
		this.slot = slot;
		this.fashionManager = fashionManager;
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
		configureButton(lockButton, ICON_SIZE);
		lockButton.addActionListener(e -> fashionManager.toggleItemLocked(slot));
		buttons.add(lockButton);

		xButton = new JButton();
		configureButton(xButton, ICON_SIZE);
		xButton.setIcon(PanelUtil.icon("x"));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> fashionManager.revertSlot(slot)));
		buttons.add(xButton);

		icon.setToolTipText("Open " + slot.name().toLowerCase() + " slot search");

		rightPanel.add(buttons, BorderLayout.EAST);

		add(rightPanel, BorderLayout.CENTER);
		refreshData();
	}

	void onItemChanged(ItemChanged e)
	{
		if (e.getModelType() == ModelType.VIRTUAL && e.getSlot() == slot)
		{
			updateItem();
			updateXButton();
		}
	}

	void onLockChanged(LockChanged e)
	{
		if (e.getSlot() == slot)
		{
			updateLockButton();
		}
	}

	private void refreshData()
	{
		updateItem();
		updateXButton();
		updateLockButton();
	}

	private void updateItem()
	{
		SlotInfo slotInfo = fashionManager.getLayers().getVirtualModels().getItems().get(slot);
		Integer newId = slotInfo != null ? slotInfo.getItemId() : null;
		setItemName(newId);
		setItemIcon(newId);
		resetMouseListeners();
	}

	private void resetMouseListeners()
	{
		// Somehow these listeners clear when the item changes, so they're refreshed like this (ugh)
		icon.removeMouseListener(mouseAdapter);
		mouseAdapter = createOpenSearchClickListener();
		icon.addMouseListener(mouseAdapter);

		icon.removeMouseListener(hoverAdapter);
		hoverAdapter = PanelUtil.hoverCursor(this);
		icon.addMouseListener(hoverAdapter);
	}

	void updateLockButton()
	{
		PanelUtil.applyLockButton(lockButton, fashionManager.isItemLocked(slot),
			slot.name().toLowerCase() + " slot");
	}

	void updateXButton()
	{
		boolean occupied = fashionManager.getLayers().getVirtualModels().getItems().containsKey(slot);
		xButton.setEnabled(occupied);
		xButton.setToolTipText("Clear " + slot.name().toLowerCase() + " slot");
	}

	private void setItemIcon(Integer itemId)
	{
		if (itemId != null && itemId >= 0)
		{
			clientThread.invokeLater(() -> {
				// this can throw if run early in game client lifecycle
				try
				{
					ItemComposition itemComposition = itemManager.getItemComposition(itemId);
					AsyncBufferedImage image = itemManager.getImage(itemComposition.getId());
					image.addTo(icon);
				}
				catch (Exception e)
				{
					return false;
				}
				return true;
			});
		}
		else
		{
			BufferedImage image = ImageUtil.loadImageResource(getClass(), slot.name().toLowerCase() + ".png");
			setIcon(icon, image);
		}
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

}
