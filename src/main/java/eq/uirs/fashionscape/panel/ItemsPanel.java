package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.WeaponAnimMismatch;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.KnownKitChanged;
import eq.uirs.fashionscape.core.event.LockChanged;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
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
import net.runelite.client.util.LinkBrowser;
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

	private static final String ISSUE_URL = "https://github.com/equirs/fashionscape-data/issues/new";

	private final Set<KitType> unknownSlots = new HashSet<>();
	private final List<ItemPanel> itemPanels = new ArrayList<>();
	// resolved names for weapons whose observed idle anim disagrees with fashionscape's data
	private final List<WeaponMismatch> weaponMismatches = new ArrayList<>();

	private JPanel slotsPanel;
	private JPanel warningsPanel;

	@Value
	private static class SlotResult
	{
		KitType slot;
		BufferedImage image;
	}

	@Value
	private static class WeaponMismatch
	{
		String name;
		WeaponAnimMismatch info;
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
			itemPanels.add(itemPanel);
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(itemPanel, BorderLayout.NORTH);
			slotsPanel.add(marginWrapper, c);
			c.gridy++;
		}
	}

	void onItemChanged(ItemChanged e)
	{
		itemPanels.forEach(p -> p.onItemChanged(e));
	}

	void onLockChanged(LockChanged e)
	{
		itemPanels.forEach(p -> p.onLockChanged(e));
	}

	void onKnownKitChanged(KnownKitChanged e)
	{
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
	}

	// re-derives which real weapons have idle anims that disagree with fashionscape's internal data
	void refreshWeaponWarnings()
	{
		clientThread.invokeLater(() -> {
			List<WeaponMismatch> resolved = fashionManager.getWeaponAnimMismatches().stream()
				.map(m -> new WeaponMismatch(itemManager.getItemComposition(m.getItemId()).getName(), m))
				.collect(Collectors.toList());
			SwingUtilities.invokeLater(() -> {
				weaponMismatches.clear();
				weaponMismatches.addAll(resolved);
				refreshWarnings();
			});
		});
	}

	public void refreshWarnings()
	{
		warningsPanel.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;

		if (!unknownSlots.isEmpty())
		{
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

		if (!weaponMismatches.isEmpty())
		{
			JLabel label = new JLabel("<html>These weapons animate differently from fashionscape's data. " +
				"Please report these so they can be fixed:</html>");
			label.setPreferredSize(new Dimension(100, 60));
			label.setForeground(ColorScheme.BRAND_ORANGE);
			warningsPanel.add(label, c);
			c.gridy++;

			weaponMismatches.stream()
				.sorted(Comparator.comparing(WeaponMismatch::getName))
				.forEach(m -> {
					JLabel itemLabel = new JLabel(String.format("id %d: anim %d",
						m.getInfo().getItemId(), m.getInfo().getObservedAnimId()));
					itemLabel.setForeground(ColorScheme.BRAND_ORANGE);
					itemLabel.setToolTipText(m.getName());
					warningsPanel.add(itemLabel, c);
					c.gridy++;
				});

			JButton reportButton = new JButton("Report on GitHub");
			reportButton.setFocusPainted(false);
			reportButton.addActionListener(e -> LinkBrowser.open(buildIssueUrl()));
			reportButton.addMouseListener(PanelUtil.hoverCursor(reportButton));
			c.insets = new Insets(5, 0, 0, 0);
			warningsPanel.add(reportButton, c);
			c.gridy++;
		}

		revalidate();
	}

	private String buildIssueUrl()
	{
		String body = "The following weapons have mismatched idle animations:\n\n" +
			weaponMismatches.stream()
				.sorted(Comparator.comparing(WeaponMismatch::getName))
				.map(m -> String.format("- %s (id %d): observed %d, expected %d",
					m.getName(), m.getInfo().getItemId(), m.getInfo().getObservedAnimId(),
					m.getInfo().getDataAnimId()))
				.collect(Collectors.joining("\n"));
		return ISSUE_URL +
			"?title=" + URLEncoder.encode("Incorrect weapon idle animation(s)", StandardCharsets.UTF_8) +
			"&body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
	}
}
