package eq.uirs.fashionscape.panel.search;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.LockStatus;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.SwingUtil;

/**
 * Tab panel that houses the search UI: bar, filters, sort, results, etc.
 */
@Slf4j
public class SearchPanel extends JPanel
{
	private static final int DEBOUNCE_DELAY_MS = 200;
	private static final String ERROR_PANEL = "ERROR_PANEL";
	private static final String RESULTS_PANEL = "RESULTS_PANEL";

	private final FashionManager fashionManager;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final ScheduledExecutorService executor;
	private final ItemSearch itemSearch;
	private final boolean developerMode;

	// constrain items in the list of results
	private final GridBagConstraints itemConstraints = new GridBagConstraints();
	private final CardLayout cardLayout = new CardLayout();
	private final IconTextField searchBar = new IconTextField();
	private final MaterialTabGroup slotFilter = new MaterialTabGroup();
	private final JPanel resultsPanel = new JPanel();
	private final JScrollPane resultsScrollPane;
	private final JPanel centerPanel = new JPanel(cardLayout);
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();
	private final Map<PanelEquipSlot, MaterialTab> tabMap;
	private final List<SearchItemPanel> searchPanels = new ArrayList<>();

	private final AtomicBoolean searchInProgress = new AtomicBoolean();
	private final OnSelectionChangingListener listener = new OnSelectionChangingListener()
	{
		@Override
		public void onSearchSelectionChanging(KitType slot)
		{
			for (SearchItemPanel item : searchPanels)
			{
				if (Objects.equals(item.itemId, fashionManager.virtualItemIdFor(slot)) ||
					(item.itemId < 0 && fashionManager.isNothing(slot)))
				{
					item.resetBackground();
				}
			}
		}
	};

	private Future<?> searchFuture = null;
	// null -> any slot. Set on the EDT, read by searches on the client thread
	@Nullable
	private volatile KitType slot = null;
	private SortBy sort;
	private boolean sortInverted;
	private boolean hasSearched = false;

	@Inject
	public SearchPanel(FashionManager fashionManager, ClientThread clientThread, ItemManager itemManager,
					   ScheduledExecutorService executor, ItemSearch itemSearch, FashionscapeConfig config,
					   @Named("developerMode") boolean developerMode)
	{
		this.fashionManager = fashionManager;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.executor = executor;
		this.itemSearch = itemSearch;
		this.developerMode = developerMode;
		this.sort = config.preferredSort();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// constrain top-level view groups
		GridBagConstraints groupConstraints = new GridBagConstraints();
		groupConstraints.fill = GridBagConstraints.HORIZONTAL;
		groupConstraints.weightx = 1;
		groupConstraints.weighty = 0;
		groupConstraints.gridx = 0;
		groupConstraints.gridy = 0;

		itemConstraints.fill = GridBagConstraints.HORIZONTAL;
		itemConstraints.weightx = 1;
		itemConstraints.weighty = 0;
		itemConstraints.gridx = 0;
		itemConstraints.gridy = 0;

		JPanel container = new JPanel();
		container.setLayout(new GridBagLayout());
		container.setBorder(new EmptyBorder(10, 10, 10, 10));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		this.tabMap = setUpSlotFilters();
		setUpSearchBar();

		resultsPanel.setLayout(new GridBagLayout());
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(resultsPanel, BorderLayout.NORTH);

		resultsScrollPane = new JScrollPane(wrapper);
		resultsScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsScrollPane.setVisible(false);

		JPanel infoWrapper = new JPanel(new BorderLayout());
		infoWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		errorPanel.setContent("Equipment Search", "Items you can try on will appear here.");
		infoWrapper.add(errorPanel, BorderLayout.NORTH);

		centerPanel.add(resultsScrollPane, RESULTS_PANEL);
		centerPanel.add(infoWrapper, ERROR_PANEL);

		cardLayout.show(centerPanel, ERROR_PANEL);

		// label takes its natural width, combo box fills the rest, direction button is fixed-width
		JPanel sortBar = new JPanel(new BorderLayout(5, 0));
		sortBar.setBorder(new EmptyBorder(5, 0, 0, 0));
		sortBar.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel sortLabel = new JLabel("Sort by");
		sortLabel.setForeground(Color.WHITE);
		sortBar.add(sortLabel, BorderLayout.WEST);
		sortBar.add(createSortBox(config), BorderLayout.CENTER);
		sortBar.add(createSortDirectionButton(), BorderLayout.EAST);

		container.add(slotFilter, groupConstraints);
		groupConstraints.gridy++;
		container.add(searchBar, groupConstraints);
		groupConstraints.gridy++;
		container.add(sortBar, groupConstraints);
		groupConstraints.gridy++;

		add(container, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(searchBar::requestFocusInWindow);
	}

	public void onLockChanged(LockChanged e)
	{
		updateTabIcon(e.getSlot(), e.getStatus());
	}

	private JComboBox<SortBy> createSortBox(FashionscapeConfig config)
	{
		JComboBox<SortBy> sortBox = new JComboBox<>(SortBy.values());
		sortBox.setSelectedItem(this.sort);
		sortBox.setPreferredSize(new Dimension(sortBox.getPreferredSize().width, 25));
		sortBox.setForeground(Color.WHITE);
		sortBox.setFocusable(false);
		sortBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				SortBy selectedSort = (SortBy) sortBox.getSelectedItem();
				config.setPreferredSort(selectedSort);
				sort = selectedSort;
				updateSearchDebounced();
			}
		});
		return sortBox;
	}

	private JButton createSortDirectionButton()
	{
		JButton button = new JButton();
		button.setPreferredSize(new Dimension(25, 25));
		button.setFocusPainted(false);
		button.addMouseListener(PanelUtil.hoverCursor(this));
		applySortDirection(button);
		button.addActionListener(e -> {
			sortInverted = !sortInverted;
			applySortDirection(button);
			updateSearchDebounced();
		});
		return button;
	}

	private void applySortDirection(JButton button)
	{
		button.setText(sortInverted ? "⬆" : "⬇");
		button.setToolTipText(sortInverted ? "Inverted order" : "Standard order");
	}

	public void clearResults()
	{
		searchPanels.clear();
		SwingUtil.fastRemoveAll(resultsPanel);
	}

	public void reloadResults()
	{
		updateSearchDebounced();
		SwingUtilities.invokeLater(searchBar::requestFocusInWindow);
	}

	public void chooseSlot(KitType slot)
	{
		PanelEquipSlot panelSlot = Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.orElse(PanelEquipSlot.ALL);
		MaterialTab tab = tabMap.get(panelSlot);
		if (tab != null)
		{
			slotFilter.select(tab);
		}
	}

	public void clearSearch()
	{
		if (!Strings.isNullOrEmpty(searchBar.getText()))
		{
			searchBar.setText("");
		}
		searchPanels.clear();
		SwingUtilities.invokeLater(() -> {
			SwingUtil.fastRemoveAll(resultsPanel);
			resultsPanel.updateUI();
			searchBar.requestFocusInWindow();
		});
	}

	private Map<PanelEquipSlot, MaterialTab> setUpSlotFilters()
	{
		ImmutableMap.Builder<PanelEquipSlot, MaterialTab> builder = new ImmutableMap.Builder<>();
		slotFilter.setLayout(new GridLayout(2, 5, 5, 5));
		slotFilter.setBorder(new EmptyBorder(0, 0, 10, 0));
		MaterialTab allTab = null;
		for (PanelEquipSlot filterSlot : PanelEquipSlot.values())
		{
			MaterialTab tab = new MaterialTab(new ImageIcon(), slotFilter, null);
			builder.put(filterSlot, tab);
			boolean isLocked = false;
			if (filterSlot.getKitType() == null)
			{
				allTab = tab;
			}
			else
			{
				isLocked = fashionManager.isItemLocked(filterSlot.getKitType());
			}
			ImageIcon icon = iconFor(filterSlot, isLocked);
			tab.setIcon(icon);
			tab.setToolTipText(filterSlot.getDisplayName());
			tab.addMouseListener(PanelUtil.hoverCursor(this));
			tab.setOnSelectEvent(() -> {
				slot = filterSlot.getKitType();
				// reset scroll position
				updateSearchDebounced(() -> resultsScrollPane.getVerticalScrollBar().setValue(0));
				return true;
			});
			slotFilter.addTab(tab);
		}
		if (allTab != null)
		{
			slotFilter.select(allTab);
		}
		return builder.build();
	}

	private void setUpSearchBar()
	{
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(100, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addActionListener(e -> updateSearchDebounced());
		searchBar.addClearListener(this::clearSearch);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				hasSearched = true;
				updateSearchDebounced();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				hasSearched = true;
				updateSearchDebounced();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				hasSearched = true;
				updateSearchDebounced();
			}
		});
	}

	private void updateSearchDebounced()
	{
		updateSearchDebounced(() -> {
			// no-op
		});
	}

	private void updateSearchDebounced(Runnable postExec)
	{
		Future<?> future = searchFuture;
		if (searchFuture != null)
		{
			future.cancel(false);
		}
		searchFuture = executor.schedule(() -> clientThread.invokeLater(() -> {
			if (!updateSearch(postExec))
			{
				// search was in progress; try again later
				updateSearchDebounced(postExec);
			}
		}), DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	// should only be called from debouncer and on client thread
	private boolean updateSearch(Runnable postExec)
	{
		if (!searchInProgress.compareAndSet(false, true))
		{
			return false;
		}
		String query = searchBar.getText().toLowerCase();
		KitType searchSlot = slot;
		// a single slot narrows the results enough to list every item
		if (searchSlot == null && query.length() < 2)
		{
			showQueryTooShort();
			return true;
		}
		List<SearchResult> found = itemSearch.find(query, searchSlot);
		SortBy searchSort = sort;
		boolean inverted = sortInverted;
		executor.submit(() -> showResults(itemSearch.sort(found, searchSort, inverted), searchSlot, postExec));
		return true;
	}

	private void showQueryTooShort()
	{
		SwingUtilities.invokeLater(() -> {
			searchPanels.clear();
			SwingUtil.fastRemoveAll(resultsPanel);
			resultsPanel.updateUI();
			if (hasSearched)
			{
				errorPanel.setContent("Search too short", "Type a longer search for results");
			}
			cardLayout.show(centerPanel, ERROR_PANEL);
			searchInProgress.set(false);
		});
	}

	// only to be called from updateSearch
	private void showResults(List<SearchResult> results, @Nullable KitType searchSlot, Runnable postExec)
	{
		SwingUtilities.invokeLater(() -> {
			searchPanels.clear();
			SwingUtil.fastRemoveAll(resultsPanel);
			if (results.isEmpty())
			{
				String slotName = searchSlot != null ? searchSlot.name().toLowerCase() : "any";
				errorPanel.setContent("No results",
					"No items match \"" + searchBar.getText() + "\" in " + slotName + " slot");
				cardLayout.show(centerPanel, ERROR_PANEL);
			}
			else
			{
				boolean firstItem = true;
				for (SearchResult result : results)
				{
					SearchItemPanel panel = new SearchItemPanel(result.getId(), result.getIcon(), result.getSlot(),
						itemManager, fashionManager, clientThread, listener, result.getScore(), developerMode);
					searchPanels.add(panel);
					int topPadding = firstItem ? 0 : 5;
					firstItem = false;
					JPanel marginWrapper = new JPanel(new BorderLayout());
					marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
					marginWrapper.setBorder(new EmptyBorder(topPadding, 10, 0, 10));
					marginWrapper.add(panel, BorderLayout.NORTH);
					resultsPanel.add(marginWrapper, itemConstraints);
					itemConstraints.gridy++;
				}
				cardLayout.show(centerPanel, RESULTS_PANEL);
				resultsPanel.updateUI();
			}
			searchInProgress.set(false);
			postExec.run();
		});
	}

	private void updateTabIcon(KitType slot, @Nullable LockStatus status)
	{
		Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.ifPresent(panelEquipSlot -> updateTabIcon(panelEquipSlot, status != null));
	}

	private void updateTabIcon(PanelEquipSlot panelSlot, boolean isLocked)
	{
		MaterialTab tab = tabMap.get(panelSlot);
		if (tab != null)
		{
			tab.setIcon(iconFor(panelSlot, isLocked));
		}
	}

	private ImageIcon iconFor(PanelEquipSlot panelSlot, boolean isLocked)
	{
		String lockStr = isLocked ? "-lock" : "";
		String iconName = panelSlot.getDisplayName().toLowerCase() + lockStr;
		return PanelUtil.icon(iconName);
	}
}
