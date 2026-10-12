package eq.uirs.fashionscape.panel.search;

import com.google.common.base.Strings;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.SwingUtil;

/**
 * Tab panel that houses the search UI: bar, filters, sort, results, etc.
 */
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
	private final FilterStore filterStore;
	private final boolean developerMode;

	// constrain items in the list of results
	private final GridBagConstraints itemConstraints = new GridBagConstraints();
	private final CardLayout cardLayout = new CardLayout();
	private final IconTextField searchBar = new IconTextField();
	private final JPanel resultsPanel = new JPanel();
	private final JScrollPane resultsScrollPane;
	private final JPanel centerPanel = new JPanel(cardLayout);
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();
	private final JButton resetFiltersButton = new JButton("Reset filters");
	private final JLabel resultCount = new JLabel();
	private final FilterPanel filterPanel;
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
	// set on the EDT, read by searches on the client thread. null -> any slot
	@Nullable
	private volatile KitType slot = null;
	private volatile ItemFilters filters;
	private SortBy sort;
	private boolean sortInverted;

	@Inject
	public SearchPanel(FashionManager fashionManager, ClientThread clientThread, ItemManager itemManager,
					   ScheduledExecutorService executor, ItemSearch itemSearch, FilterStore filterStore,
					   FashionscapeConfig config, @Named("developerMode") boolean developerMode)
	{
		this.fashionManager = fashionManager;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.executor = executor;
		this.itemSearch = itemSearch;
		this.filterStore = filterStore;
		this.filters = filterStore.load();
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

		// selects the "all" slot initially, which runs the first search
		filterPanel = new FilterPanel(fashionManager, filters, this::onSlotChanged, this::onFiltersChanged);
		infoWrapper.add(createResetFiltersRow(), BorderLayout.CENTER);

		JPanel filterBar = new JPanel(new GridLayout(1, 2, 5, 0));
		filterBar.setBorder(new EmptyBorder(5, 0, 0, 0));
		filterBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filterBar.add(filterPanel.getSlotToggle());
		filterBar.add(filterPanel.getFiltersToggle());

		// label takes up natural width, combo box fills, direction button is fixed-width
		JPanel sortBar = new JPanel(new BorderLayout(5, 0));
		sortBar.setBorder(new EmptyBorder(5, 0, 0, 0));
		sortBar.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel sortLabel = new JLabel("Sort by");
		sortLabel.setForeground(Color.WHITE);
		sortBar.add(sortLabel, BorderLayout.WEST);
		sortBar.add(createSortBox(config), BorderLayout.CENTER);
		sortBar.add(createSortDirectionButton(), BorderLayout.EAST);

		container.add(searchBar, groupConstraints);
		groupConstraints.gridy++;
		container.add(filterBar, groupConstraints);
		groupConstraints.gridy++;
		container.add(filterPanel, groupConstraints);
		groupConstraints.gridy++;
		container.add(sortBar, groupConstraints);
		groupConstraints.gridy++;

		resultCount.setFont(FontManager.getRunescapeSmallFont());
		resultCount.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resultCount.setBorder(new EmptyBorder(5, 0, 0, 0));
		resultCount.setVisible(false);
		container.add(resultCount, groupConstraints);
		groupConstraints.gridy++;

		add(container, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(searchBar::requestFocusInWindow);
	}

	public void onLockChanged(LockChanged e)
	{
		filterPanel.onLockChanged(e);
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
		filterPanel.selectSlot(slot);
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

	private void onSlotChanged(@Nullable KitType newSlot)
	{
		slot = newSlot;
		updateSearchFromTop();
	}

	private void onFiltersChanged(ItemFilters newFilters)
	{
		filters = newFilters;
		filterStore.save(newFilters);
		updateSearchFromTop();
	}

	private void updateSearchFromTop()
	{
		updateSearchDebounced(() -> resultsScrollPane.getVerticalScrollBar().setValue(0));
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
				updateSearchDebounced();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSearchDebounced();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
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
		ItemFilters searchFilters = filters;
		List<SearchResult> found = itemSearch.find(query, searchSlot, searchFilters);
		// "any slot" can show as many results as the largest slot tab, but no more
		if (searchSlot == null && found.size() > itemSearch.largestSlotSize())
		{
			showTooManyResults(found.size());
			return true;
		}
		SortBy searchSort = sort;
		boolean inverted = sortInverted;
		boolean filtered = searchFilters.activeCount() > 0;
		if (searchSort == SortBy.COLOR_MATCH)
		{
			executor.submit(() ->
				showResults(itemSearch.sort(found, searchSort, inverted), searchSlot, filtered, postExec));
		}
		else
		{
			showResults(itemSearch.sort(found, searchSort, inverted), searchSlot, filtered, postExec);
		}
		return true;
	}

	private void showTooManyResults(int count)
	{
		SwingUtilities.invokeLater(() -> {
			searchPanels.clear();
			SwingUtil.fastRemoveAll(resultsPanel);
			resultsPanel.updateUI();
			resultCount.setVisible(false);
			resetFiltersButton.setVisible(false);
			errorPanel.setContent("Too many results", QuantityFormatter.formatNumber(count) +
				" items match. Pick a slot, add filters or try a longer search.");
			cardLayout.show(centerPanel, ERROR_PANEL);
			searchInProgress.set(false);
		});
	}

	// only to be called from updateSearch
	private void showResults(List<SearchResult> results, @Nullable KitType searchSlot, boolean filtered,
							 Runnable postExec)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				displayResults(results, searchSlot, filtered);
				postExec.run();
			}
			finally
			{
				searchInProgress.set(false);
			}
		});
	}

	private void displayResults(List<SearchResult> results, @Nullable KitType searchSlot, boolean filtered)
	{
		searchPanels.clear();
		SwingUtil.fastRemoveAll(resultsPanel);
		resultCount.setVisible(!results.isEmpty());
		resultCount.setText(QuantityFormatter.formatNumber(results.size()) +
			(results.size() == 1 ? " result" : " results"));
		if (results.isEmpty())
		{
			errorPanel.setContent("No results", noResultsMessage(searchSlot, filtered));
			resetFiltersButton.setVisible(filtered);
			cardLayout.show(centerPanel, ERROR_PANEL);
		}
		else
		{
			boolean firstItem = true;
			for (SearchResult result : results)
			{
				SearchItemPanel panel = new SearchItemPanel(result, iconFor(result), itemManager, fashionManager,
					clientThread, listener, developerMode);
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
	}

	// e.g. No items match "abc" in the head slot with the current filters
	private String noResultsMessage(@Nullable KitType searchSlot, boolean filtered)
	{
		String query = searchBar.getText();
		if (query.isEmpty() && searchSlot == null && !filtered)
		{
			return "No items found";
		}
		StringBuilder message = new StringBuilder(query.isEmpty() ? "No items" : "No items match \"" + query + "\"");
		if (searchSlot != null)
		{
			message.append(" in the ").append(searchSlot.name().toLowerCase()).append(" slot");
		}
		if (filtered)
		{
			message.append(query.isEmpty() ? " match" : " with").append(" the current filters");
		}
		return message.toString();
	}

	private JPanel createResetFiltersRow()
	{
		resetFiltersButton.setFocusPainted(false);
		resetFiltersButton.setVisible(false);
		resetFiltersButton.addMouseListener(PanelUtil.hoverCursor(resetFiltersButton));
		resetFiltersButton.addActionListener(e -> filterPanel.resetFilters());
		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.add(resetFiltersButton);
		return row;
	}

	private BufferedImage iconFor(SearchResult result)
	{
		return result.getItemComposition() != null ? itemManager.getImage(result.getId()) :
			PanelUtil.image(result.getSlot().name().toLowerCase());
	}
}
