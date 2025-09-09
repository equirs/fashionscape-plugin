package eq.uirs.fashionscape.panel;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * Tab panel that houses the search UI: bar, filters, sort, results, etc.
 */
class SearchPanel extends JPanel
{
	private static final int DEBOUNCE_DELAY_MS = 200;
	private static final String ERROR_PANEL = "ERROR_PANEL";
	private static final String RESULTS_PANEL = "RESULTS_PANEL";
	private static final Set<Integer> VALID_SLOT_IDS = Arrays.stream(PanelEquipSlot.values())
		.map(PanelEquipSlot::getKitType)
		.filter(Objects::nonNull)
		.map(KitType::getIndex)
		.collect(Collectors.toSet());

	private final Client client;
	private final FashionManager fashionManager;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final FashionscapeConfig config;
	private final ScheduledExecutorService executor;
	private final ColorScorer colorScorer;

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

	private final List<Result> results = new ArrayList<>();
	private final AtomicBoolean searchInProgress = new AtomicBoolean();
	private final OnSelectionChangingListener listener = new OnSelectionChangingListener()
	{
		@Override
		public void onSearchSelectionChanging(KitType slot)
		{
			for (SearchItemPanel item : searchPanels)
			{
				if (Objects.equals(item.itemId, fashionManager.swappedItemIdIn(slot)) ||
					(item.itemId != null && item.itemId < 0 && fashionManager.isHidden(slot)))
				{
					item.resetBackground();
				}
			}
		}
	};

	private Future<?> searchFuture = null;
	private Function<ItemComposition, Boolean> filter;
	private boolean allowShortQueries = false;
	private SortBy sort;
	private KitType selectedSlot = null;
	private boolean hasSearched = false;
	private final Map<Integer, Double> scores = new HashMap<>();

	private final Comparator<Result> itemAlphaComparator = Comparator.comparing(Result::getName);

	@Value
	private static class Result
	{
		@Nullable
		ItemComposition itemComposition;
		BufferedImage icon;
		KitType slot;

		int getId() {
			return itemComposition != null ? itemComposition.getId() : NothingItemComposition.ID;
		}

		String getName() {
			return itemComposition != null ? itemComposition.getMembersName() : NothingItemComposition.NAME;
		}
	}

	@Inject
	public SearchPanel(Client client, FashionManager fashionManager, ClientThread clientThread,
					   ItemManager itemManager, ScheduledExecutorService executor,
					   FashionscapeConfig config, ColorScorer colorScorer)
	{
		this.client = client;
		this.fashionManager = fashionManager;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.executor = executor;
		this.config = config;
		this.colorScorer = colorScorer;
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
		resultsScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		resultsScrollPane.setVisible(false);

		JPanel infoWrapper = new JPanel(new BorderLayout());
		infoWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		errorPanel.setContent("Equipment Search", "Items you can try on will appear here.");
		infoWrapper.add(errorPanel, BorderLayout.NORTH);

		centerPanel.add(resultsScrollPane, RESULTS_PANEL);
		centerPanel.add(infoWrapper, ERROR_PANEL);

		cardLayout.show(centerPanel, ERROR_PANEL);

		JPanel sortBar = new JPanel();
		sortBar.setLayout(new GridLayout(1, 2));
		sortBar.setBorder(new EmptyBorder(5, 0, 0, 0));
		sortBar.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel sortLabel = new JLabel("Sort by");
		sortLabel.setForeground(Color.WHITE);
		sortLabel.setMaximumSize(new Dimension(0, 0));
		sortLabel.setPreferredSize(new Dimension(0, 0));
		sortBar.add(sortLabel);

		JComboBox<SortBy> sortBox = createSortBox(config);
		sortBar.add(sortBox);

		container.add(slotFilter, groupConstraints);
		groupConstraints.gridy++;
		container.add(searchBar, groupConstraints);
		groupConstraints.gridy++;
		container.add(sortBar, groupConstraints);
		groupConstraints.gridy++;

		add(container, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(searchBar::requestFocusInWindow);

		fashionManager.addEventListener(new LockChangedListener((e) -> {
			if (e.getType() != LockChanged.Type.KIT)
			{
				updateTabIcon(e);
			}
		}));
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
		results.clear();
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
			tab.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
			tab.setOnSelectEvent(() -> {
				filter = itemComposition -> {
					KitType kitType = filterSlot.getKitType();
					selectedSlot = kitType;
					Integer slotId = fashionManager.slotIdFor(itemComposition);
					if (kitType == null)
					{
						// allow any equipment slot that is also a KitType (so no ammo, etc)
						return slotId != null && VALID_SLOT_IDS.contains(slotId);
					}
					else
					{
						return slotId != null && kitType.getIndex() == slotId;
					}
				};
				// individual slots will show all results all the time
				allowShortQueries = filterSlot.getKitType() != null;
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
		boolean willUpdate = searchInProgress.compareAndSet(false, true);
		if (willUpdate)
		{
			results.clear();

			String search = searchBar.getText().toLowerCase();
			if (!allowShortQueries && search.length() < 2)
			{
				searchPanels.clear();
				SwingUtilities.invokeLater(() -> {
					SwingUtil.fastRemoveAll(resultsPanel);
					resultsPanel.updateUI();
					if (hasSearched)
					{
						errorPanel.setContent("Search too short", "Type a longer search for results");
					}
					cardLayout.show(centerPanel, ERROR_PANEL);
					searchInProgress.set(false);
				});
				return true;
			}

			Set<Integer> ids = new HashSet<>();
			Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
			for (int i = 0; i < client.getItemCount(); i++)
			{
				ItemComposition itemComposition = null;
				ItemStats stats = null;
				try
				{
					int canonical = itemManager.canonicalize(i);
					if (skips.contains(canonical))
					{
						continue;
					}
					itemComposition = itemManager.getItemComposition(canonical);
					stats = itemManager.getItemStats(canonical);
				}
				catch (Exception ignored)
				{
				}
				// id might already be in results due to canonicalize
				if (itemComposition != null && stats != null && stats.isEquipable() &&
					!ids.contains(itemComposition.getId()) && isValidSearch(itemComposition, search))
				{
					ids.add(itemComposition.getId());
					try
					{
						KitType slot = KitType.values()[stats.getEquipment().getSlot()];
						AsyncBufferedImage image = itemManager.getImage(itemComposition.getId());
						results.add(new Result(itemComposition, image, slot));
					}
					catch (Exception ignored)
					{
					}
				}
			}
			if (selectedSlot != null && FashionManager.ALLOWS_NOTHING.contains(selectedSlot) &&
				NothingItemComposition.NAME.toLowerCase().contains(search))
			{
				BufferedImage image = ImageUtil.loadImageResource(getClass(), selectedSlot.name().toLowerCase() + ".png");
				results.add(0, new Result(null, image, selectedSlot));
			}

			searchPanels.clear();
			scores.clear();
			switch (this.sort)
			{
				case RELEASE:
					addPendingResults(postExec);
					break;
				case ALPHABETICAL:
					executor.submit(() -> {
						results.sort(itemAlphaComparator);
						addPendingResults(postExec);
					});
					break;
				case COLOR_MATCH:
					executor.submit(() -> {
						performSuggestedSort();
						addPendingResults(postExec);
					});
					break;
			}
		}
		return willUpdate;
	}

	private void performSuggestedSort()
	{
		colorScorer.updatePlayerInfo();
		for (Result result : results)
		{
			int itemId = result.getId();
			scores.put(itemId, colorScorer.score(itemId, selectedSlot));
		}
		results.sort(Comparator.comparing(r ->
			-scores.getOrDefault(r.getId(), 0.0)));
	}

	// only to be called from updateSearch
	private void addPendingResults(Runnable postExec)
	{
		SwingUtilities.invokeLater(() -> {
			SwingUtil.fastRemoveAll(resultsPanel);
			if (results.isEmpty())
			{
				String slotName = "any";
				if (selectedSlot != null)
				{
					slotName = selectedSlot.name().toLowerCase();
				}
				errorPanel.setContent("No results",
					"No items match \"" + searchBar.getText() + "\" in " + slotName + " slot");
				cardLayout.show(centerPanel, ERROR_PANEL);
			}
			else
			{
				boolean firstItem = true;
				boolean showScores = true;
				for (Result result : results)
				{
					Integer itemId = result.getId();
					Double score = scores.get(itemId);
					if (firstItem)
					{
						showScores = score != null && score != 0.0;
					}
					if (!showScores)
					{
						score = null;
					}
					SearchItemPanel panel = new SearchItemPanel(itemId, result.getIcon(),
						result.getSlot(), itemManager, fashionManager, clientThread, listener, score);
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

	private boolean isValidSearch(ItemComposition itemComposition, String query)
	{
		String name = itemComposition.getMembersName().toLowerCase();
		// The client assigns "null" to item names of items it doesn't know about
		if (name.equals("null") || !name.contains(query))
		{
			return false;
		}
		return filter == null || filter.apply(itemComposition);
	}

	private void updateTabIcon(LockChanged event)
	{
		KitType slot = event.getSlot();
		boolean isLocked = event.isLocked();
		Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.ifPresent(panelEquipSlot -> updateTabIcon(panelEquipSlot, isLocked));
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
		String iconName = panelSlot.getDisplayName().toLowerCase() + lockStr + ".png";
		return new ImageIcon(ImageUtil.loadImageResource(getClass(), iconName));
	}

}
