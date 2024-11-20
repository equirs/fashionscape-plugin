package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.KnownKitChangedListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

/**
 * Tab panel for managing virtual items
 */
@Slf4j
class ItemsPanel extends JPanel
{
	private final FashionManager fashionManager;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final SearchOpener searchOpener;
	private final boolean developerMode;

	private final Set<KitType> unknownSlots = new HashSet<>();

	private JPanel slotsPanel;
	private JPanel warningsPanel;

	@Value
	private static class SlotResult
	{
		KitType slot;
		BufferedImage image;
	}

	public ItemsPanel(FashionManager fashionManager, ItemManager itemManager, SearchOpener searchOpener,
					  ClientThread clientThread, boolean developerMode)
	{
		this.fashionManager = fashionManager;
		this.itemManager = itemManager;
		this.searchOpener = searchOpener;
		this.clientThread = clientThread;
		this.developerMode = developerMode;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		JPanel contentPanel = setUpContentPanel();
		JPanel scrollWrapper = new JPanel(new BorderLayout());
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.add(contentPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scrollPane.setViewportView(scrollWrapper);

		add(scrollPane, BorderLayout.CENTER);

		// this panel is never recreated before shutdown, and its listener must remain on
		Events.addListener(new KnownKitChangedListener(e -> {
			KitType slot = e.getSlot();
			if (e.isUnknown())
			{
				this.unknownSlots.add(slot);
			}
			else
			{
				this.unknownSlots.remove(slot);
			}
			SwingUtilities.invokeLater(this::refreshWarnings);
		}));
	}

	private JPanel setUpContentPanel()
	{
		JPanel contentContainer = new JPanel();
		contentContainer.setLayout(new GridBagLayout());
		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 1, 0, 1);
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		slotsPanel = new JPanel();
		slotsPanel.setLayout(new GridBagLayout());
		slotsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		populateSlots();
		contentContainer.add(slotsPanel, c);
		c.gridy++;

		warningsPanel = new JPanel();
		warningsPanel.setLayout(new GridBagLayout());
		warningsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		warningsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		refreshWarnings();
		contentContainer.add(warningsPanel, c);

		return contentContainer;
	}

	private void populateSlots()
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
				Integer itemId = fashionManager.virtualItemIdFor(slot);
				BufferedImage image;
				if (itemId != null && itemId >= 0)
				{
					ItemComposition itemComposition = itemManager.getItemComposition(itemId);
					image = itemManager.getImage(itemComposition.getId());
				}
				else
				{
					image = ImageUtil.loadImageResource(getClass(), slot.name().toLowerCase() + ".png");
				}
				slotResults.add(new SlotResult(slot, image));
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
			ItemPanel itemPanel = new ItemPanel(s.image, itemManager,
				clientThread, fashionManager, s.slot, searchOpener, developerMode);
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(itemPanel, BorderLayout.NORTH);
			slotsPanel.add(marginWrapper, c);
			c.gridy++;
		}
	}

	public void refreshWarnings()
	{
		warningsPanel.removeAll();

		if (!unknownSlots.isEmpty())
		{
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.PAGE_START;
			c.weightx = 1;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;

			JLabel label = new JLabel("<html>Some base models can't be determined. Remove " +
				"some of your in-game equipment to detect them:</html>");
			label.setPreferredSize(new Dimension(100, 60));
			label.setForeground(ColorScheme.BRAND_ORANGE);
			warningsPanel.add(label, c);
			c.gridy++;

			unknownSlots.stream()
				.sorted(Comparator.comparing(KitType::name))
				.forEach(slot -> {
					JLabel slotLabel = new JLabel(Text.titleCase(slot));
					slotLabel.setForeground(ColorScheme.BRAND_ORANGE);
					warningsPanel.add(slotLabel, c);
					c.gridy++;
				});
		}
		revalidate();
	}
}
