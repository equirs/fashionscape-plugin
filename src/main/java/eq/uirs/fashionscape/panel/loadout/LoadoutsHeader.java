package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;

/**
 * Title row, "Load current equipment" button, and sort bar above the loadout list.
 */
class LoadoutsHeader extends JPanel
{
	private final FashionManager fashionManager;
	private final LoadoutSharing sharing;
	private final FashionscapeConfig config;

	private final JButton addButton;
	@Getter
	private LoadoutSort sort;

	@Setter
	private Runnable onBack = () -> {
	};
	@Setter
	private Runnable onSave = () -> {
	};
	@Setter
	private Runnable onSortChanged = () -> {
	};

	@Inject
	LoadoutsHeader(FashionManager fashionManager, LoadoutSharing sharing, FashionscapeConfig config)
	{
		this.fashionManager = fashionManager;
		this.sharing = sharing;
		this.config = config;
		this.sort = config.loadoutSort();
		this.addButton = iconButton("add", "Save current look");
		addButton.addActionListener(e -> onSave.run());

		setLayout(new BorderLayout(0, 5));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(0, 10, 8, 10));
		add(createTitleRow(), BorderLayout.NORTH);
		add(createLoadEquipmentButton(), BorderLayout.CENTER);
		add(createSortBar(), BorderLayout.SOUTH);
	}

	void setCanSave(boolean canSave)
	{
		addButton.setEnabled(canSave);
	}

	private JPanel createTitleRow()
	{
		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);

		JButton back = iconButton("back", "Back");
		back.addActionListener(e -> onBack.run());
		titleRow.add(back, BorderLayout.WEST);

		JLabel title = new JLabel("Loadouts");
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 8, 0, 0));
		titleRow.add(title, BorderLayout.CENTER);

		JButton moreButton = iconButton("more", "Import and export");
		JPopupMenu moreMenu = createMoreMenu();
		moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		buttons.setOpaque(false);
		buttons.add(addButton);
		buttons.add(moreButton);
		titleRow.add(buttons, BorderLayout.EAST);
		return titleRow;
	}

	private JButton createLoadEquipmentButton()
	{
		JButton button = new JButton("Load current equipment");
		button.setFocusPainted(false);
		button.addMouseListener(PanelUtil.hoverCursor(this));
		button.addActionListener(e -> fashionManager.importSelf());
		return button;
	}

	private JPanel createSortBar()
	{
		JComboBox<LoadoutSort> sortBox = new JComboBox<>(LoadoutSort.values());
		sortBox.setSelectedItem(sort);
		sortBox.setPreferredSize(new Dimension(sortBox.getPreferredSize().width, 25));
		sortBox.setForeground(Color.WHITE);
		sortBox.setFocusable(false);
		sortBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				sort = (LoadoutSort) sortBox.getSelectedItem();
				config.setLoadoutSort(sort);
				onSortChanged.run();
			}
		});

		JLabel sortLabel = new JLabel("Sort by");
		sortLabel.setForeground(Color.WHITE);

		JPanel sortBar = new JPanel(new BorderLayout(5, 0));
		sortBar.setOpaque(false);
		sortBar.add(sortLabel, BorderLayout.WEST);
		sortBar.add(sortBox, BorderLayout.CENTER);
		return sortBar;
	}

	private JPopupMenu createMoreMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem importClipboard = new JMenuItem("Import from clipboard");
		importClipboard.addActionListener(e -> sharing.importClipboard(this));
		menu.add(importClipboard);
		JMenuItem importFiles = new JMenuItem("Import from file...");
		importFiles.addActionListener(e -> sharing.importFiles(this));
		menu.add(importFiles);
		menu.addSeparator();
		JMenuItem exportAll = new JMenuItem("Export all to file...");
		exportAll.addActionListener(e -> sharing.exportAll(this));
		menu.add(exportAll);
		return menu;
	}

	private JButton iconButton(String icon, String tooltip)
	{
		JButton button = new JButton(PanelUtil.icon(icon));
		button.setToolTipText(tooltip);
		button.setFocusPainted(false);
		button.addMouseListener(PanelUtil.hoverCursor(this));
		return button;
	}
}
