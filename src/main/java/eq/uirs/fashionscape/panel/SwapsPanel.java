package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

/**
 * Tab panel that houses all equipment swaps and the main controls
 */
@Slf4j
class SwapsPanel extends JPanel
{
	private final SwapManager swapManager;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final SearchOpener searchOpener;

	@Getter
	private final List<SwapItemPanel> itemPanels = new ArrayList<>();

	private JPanel slotsPanel;

	@Value
	private static class SlotResult
	{
		KitType slot;
		Integer itemId;
		BufferedImage image;
	}

	public SwapsPanel(SwapManager swapManager, ItemManager itemManager, SearchOpener searchOpener,
					  ClientThread clientThread)
	{
		this.swapManager = swapManager;
		this.itemManager = itemManager;
		this.searchOpener = searchOpener;
		this.clientThread = clientThread;

		setLayout(new GridBagLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		JPanel buttonPanel = setUpButtonPanel();
		add(buttonPanel, c);
		c.gridy++;
	}

	private JPanel setUpButtonPanel()
	{
		JPanel buttonContainer = new JPanel();
		buttonContainer.setLayout(new GridBagLayout());
		buttonContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 1, 0, 1);
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		// Add slots directly under buttons (kinda hacky but w/e, aligning panels sucks)

		slotsPanel = new JPanel();
		slotsPanel.setLayout(new GridBagLayout());
		slotsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		populateSwapSlots();
		buttonContainer.add(slotsPanel, c);

		return buttonContainer;
	}

	private void populateSwapSlots()
	{
		clientThread.invokeLater(() -> {
			List<SlotResult> slotResults = new ArrayList<>();
			for (PanelEquipSlot panelSlot : PanelEquipSlot.values())
			{
				KitType slot = panelSlot.getKitType();
				if (slot == null)
				{
					continue;
				}
				Integer itemId = swapManager.swappedItemIdIn(slot);
				BufferedImage image;
				if (itemId != null)
				{
					ItemComposition itemComposition = itemManager.getItemComposition(itemId);
					image = itemManager.getImage(itemComposition.getId());
				}
				else
				{
					image = ImageUtil.loadImageResource(getClass(), slot.name().toLowerCase() + ".png");
				}
				slotResults.add(new SlotResult(slot, itemId, image));
			}
			SwingUtilities.invokeLater(() -> addSlotItemPanels(slotResults));
		});
	}

	private void addSlotItemPanels(List<SlotResult> results)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;

		for (SlotResult s : results)
		{
			SwapItemPanel itemPanel = new SwapItemPanel(s.itemId, s.image, itemManager,
				clientThread, swapManager, s.slot, searchOpener);
			itemPanels.add(itemPanel);
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(itemPanel, BorderLayout.NORTH);
			slotsPanel.add(marginWrapper, c);
			c.gridy++;
		}
	}

}
