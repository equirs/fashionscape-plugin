package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.Pet;
import eq.uirs.fashionscape.swap.SwapManager;
import eq.uirs.fashionscape.swap.event.KnownKitChangedListener;
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
 * Tab panel that houses all equipment swaps and the main controls
 */
@Slf4j
class SwapsPanel extends JPanel
{
	@Value
	private static class SlotResult
	{
		KitType slot;
		Integer itemId;
		BufferedImage image;
	}

	@Value
	private static class PetResult
	{
		Integer petId;
		BufferedImage image;
	}

	private final SwapManager swapManager;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final SearchOpener searchOpener;

	private final Set<KitType> unknownSlots = new HashSet<>();

	private JPanel slotsPanel;
	private JPanel warningsPanel;

	public SwapsPanel(SwapManager swapManager, ItemManager itemManager, SearchOpener searchOpener,
					  ClientThread clientThread)
	{
		this.swapManager = swapManager;
		this.itemManager = itemManager;
		this.searchOpener = searchOpener;
		this.clientThread = clientThread;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		JPanel swapsPanel = setUpContentPanel();
		JPanel scrollWrapper = new JPanel(new BorderLayout());
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.add(swapsPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scrollPane.setViewportView(scrollWrapper);

		add(scrollPane, BorderLayout.CENTER);

		swapManager.addEventListener(new KnownKitChangedListener(e -> {
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
		populateSwapSlots();
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

	private void populateSwapSlots()
	{
		clientThread.invokeLater(() -> {
			List<SlotResult> slotResults = new ArrayList<>();
			PetResult petResult = null;
			for (PanelEquipSlot panelSlot : PanelEquipSlot.values())
			{
				if (panelSlot == PanelEquipSlot.PET)
				{
					BufferedImage image = null;
					Pet pet = null;
					Integer petNpcId = swapManager.swappedPetId();
					if (petNpcId != null)
					{
						pet = SwapManager.PET_MAP.get(petNpcId);
					}
					if (pet != null)
					{
						image = itemManager.getImage(pet.getItemId());
					}
					else
					{
						image = ImageUtil.loadImageResource(getClass(), "pet.png");
					}
					petResult = new PetResult(petNpcId, image);
				}
				else if (panelSlot != PanelEquipSlot.ALL)
				{
					KitType slot = panelSlot.getKitType();
					if (slot == null)
					{
						continue;
					}
					Integer itemId = swapManager.swappedItemIdIn(slot);
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
					slotResults.add(new SlotResult(slot, itemId, image));
				}
			}
			PetResult finalPetResult = petResult;
			SwingUtilities.invokeLater(() -> addSwapPanels(slotResults, finalPetResult));
		});
	}

	private void addSwapPanels(List<SlotResult> results, PetResult petResult)
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
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(itemPanel, BorderLayout.NORTH);
			slotsPanel.add(marginWrapper, c);
			c.gridy++;
		}

		Integer petId = petResult.petId;
		BufferedImage petImage = petResult.image;
		SwapPetPanel petPanel = new SwapPetPanel(petImage, petId, clientThread, swapManager, searchOpener,
			itemManager);
		JPanel marginWrapper = new JPanel(new BorderLayout());
		marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
		marginWrapper.add(petPanel, BorderLayout.NORTH);
		slotsPanel.add(marginWrapper, c);
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
