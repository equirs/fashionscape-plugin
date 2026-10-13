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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.QuantityFormatter;

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
	private final FilterStore filterStore;

	private final CardLayout cardLayout = new CardLayout();
	private final IconTextField searchBar = new IconTextField();
	private final ResultsList resultsList;
	private final JPanel centerPanel = new JPanel(cardLayout);
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();
	private final JButton resetFiltersButton = new JButton("Reset filters");
	private final JLabel resultCount = new JLabel();
	private final FilterPanel filterPanel;

	private final AtomicBoolean searchInProgress = new AtomicBoolean();
	private final OnSelectionChangingListener listener = new OnSelectionChangingListener()
	{
		@Override
		public void onSearchSelectionChanging(KitType slot)
		{
			for (SearchItemPanel item : resultsList.getItemPanels())
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

		JPanel container = new JPanel();
		container.setLayout(new GridBagLayout());
		container.setBorder(new EmptyBorder(10, 10, 10, 10));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		setUpSearchBar();

		resultsList = new ResultsList(result -> new SearchItemPanel(result, iconFor(result), itemManager,
			fashionManager, clientThread, listener, developerMode));

		JPanel infoWrapper = new JPanel(new BorderLayout());
		infoWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		errorPanel.setContent("Equipment Search", "Items you can try on will appear here.");
		infoWrapper.add(errorPanel, BorderLayout.NORTH);

		centerPanel.add(resultsList.getScrollPane(), RESULTS_PANEL);
		centerPanel.add(infoWrapper, ERROR_PANEL);

		cardLayout.show(centerPanel, ERROR_PANEL);

		// selects the "all" slot initially, which runs the first search
		filterPanel = new FilterPanel(fashionManager, filterStore, filters, this::onSlotChanged,
			this::onFiltersChanged);
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
				updateSearchDebounced(false);
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
			updateSearchDebounced(false);
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
		resultsList.clear();
	}

	public void reloadResults()
	{
		updateSearchDebounced(false);
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
		SwingUtilities.invokeLater(() -> {
			resultsList.clear();
			searchBar.requestFocusInWindow();
		});
	}

	private void onSlotChanged(@Nullable KitType newSlot)
	{
		slot = newSlot;
		updateSearchDebounced(true);
	}

	private void onFiltersChanged(ItemFilters newFilters)
	{
		filters = newFilters;
		filterStore.save(newFilters);
		updateSearchDebounced(true);
	}

	private void setUpSearchBar()
	{
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(100, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addActionListener(e -> updateSearchDebounced(false));
		searchBar.addClearListener(this::clearSearch);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSearchDebounced(false);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSearchDebounced(false);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSearchDebounced(false);
			}
		});
	}

	// fromTop resets the scroll position
	private void updateSearchDebounced(boolean fromTop)
	{
		Future<?> future = searchFuture;
		if (searchFuture != null)
		{
			future.cancel(false);
		}
		searchFuture = executor.schedule(() -> clientThread.invokeLater(() -> {
			if (!updateSearch(fromTop))
			{
				// search was in progress; try again later
				updateSearchDebounced(fromTop);
			}
		}), DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	// should only be called from debouncer and on client thread
	private boolean updateSearch(boolean fromTop)
	{
		if (!searchInProgress.compareAndSet(false, true))
		{
			return false;
		}
		guardSearch(() -> startSearch(fromTop));
		return true;
	}

	private void startSearch(boolean fromTop)
	{
		String query = searchBar.getText().toLowerCase();
		KitType searchSlot = slot;
		ItemFilters searchFilters = filters;
		List<SearchResult> found = itemSearch.find(query, searchSlot, searchFilters);
		SortBy searchSort = sort;
		boolean inverted = sortInverted;
		boolean filtered = searchFilters.activeCount() > 0;
		if (searchSort == SortBy.COLOR_MATCH)
		{
			executor.submit(() -> guardSearch(() ->
				showResults(itemSearch.sort(found, searchSort, inverted), searchSlot, filtered, fromTop)));
		}
		else
		{
			showResults(itemSearch.sort(found, searchSort, inverted), searchSlot, filtered, fromTop);
		}
	}

	// later searches wait on the in-progress flag, so a failed search should still clear it.
	private void guardSearch(Runnable step)
	{
		try
		{
			step.run();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.warn("item search failed", e);
			searchInProgress.set(false);
		}
	}

	// only to be called from updateSearch
	private void showResults(List<SearchResult> results, @Nullable KitType searchSlot, boolean filtered,
							 boolean fromTop)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				displayResults(results, searchSlot, filtered, fromTop);
			}
			finally
			{
				searchInProgress.set(false);
			}
		});
	}

	private void displayResults(List<SearchResult> results, @Nullable KitType searchSlot, boolean filtered,
								boolean fromTop)
	{
		resultCount.setVisible(!results.isEmpty());
		resultCount.setText(QuantityFormatter.formatNumber(results.size()) +
			(results.size() == 1 ? " result" : " results"));
		if (results.isEmpty())
		{
			resultsList.clear();
			errorPanel.setContent("No results", noResultsMessage(searchSlot, filtered));
			resetFiltersButton.setVisible(filtered);
			cardLayout.show(centerPanel, ERROR_PANEL);
		}
		else
		{
			resultsList.show(results);
			if (fromTop)
			{
				resultsList.scrollToTop();
			}
			cardLayout.show(centerPanel, RESULTS_PANEL);
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
