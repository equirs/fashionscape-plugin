package eq.uirs.fashionscape.panel;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;
import net.runelite.http.api.item.ItemStats;

@Slf4j
public class FashionscapeSearchPanel extends JPanel
{
	private static final int DEBOUNCE_DELAY_MS = 200;
	private static final String ERROR_PANEL = "ERROR_PANEL";
	private static final String RESULTS_PANEL = "RESULTS_PANEL";
	private static final Set<Integer> VALID_SLOT_IDS = Arrays.stream(PanelKitType.values())
		.map(PanelKitType::getKitType)
		.filter(Objects::nonNull)
		.map(KitType::getIndex)
		.collect(Collectors.toSet());

	private final Client client;
	private final SwapManager swapManager;
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
	private final JPanel centerPanel = new JPanel(cardLayout);
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();
	private final Map<PanelKitType, MaterialTab> tabMap;
	private final List<FashionscapeSearchItemPanel> searchPanels = new ArrayList<>();

	private final List<Result> results = new ArrayList<>();
	private final AtomicBoolean searchInProgress = new AtomicBoolean();
	private final OnSelectionChangingListener listener = new OnSelectionChangingListener()
	{
		@Override
		public void onSearchSelectionChanging(KitType slot)
		{
			for (FashionscapeSearchItemPanel item : searchPanels)
			{
				if (Objects.equals(item.itemId, swapManager.swappedItemIdIn(slot)))
				{
					item.resetBackground();
				}
			}
		}
	};

	private Future<?> searchFuture = null;
	private Function<ItemComposition, Boolean> filter;
	private boolean allowShortQueries = false;
	private SortBy sort = SortBy.SUGGESTED;
	private KitType selectedSlot = null;
	private boolean hasSearched = false;
	// TODO make local
	private Map<Integer, Double> scores;

	private final Comparator<Result> itemAlphaComparator = Comparator.comparing(o -> o.getItemComposition().getName());

	@Value
	private static class Result
	{
		ItemComposition itemComposition;
		AsyncBufferedImage icon;
		KitType slot;
	}

	@Inject
	public FashionscapeSearchPanel(Client client, SwapManager swapManager, ClientThread clientThread,
								   ItemManager itemManager, ScheduledExecutorService executor,
								   FashionscapeConfig config, ColorScorer colorScorer)
	{
		this.client = client;
		this.swapManager = swapManager;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.executor = executor;
		this.config = config;
		this.colorScorer = colorScorer;

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

		JScrollPane resultsWrapper = new JScrollPane(wrapper);
		resultsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsWrapper.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		resultsWrapper.setVisible(false);

		JPanel infoWrapper = new JPanel(new BorderLayout());
		infoWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		errorPanel.setContent("Equipment Search", "Items you can try on will appear here.");
		infoWrapper.add(errorPanel, BorderLayout.NORTH);

		centerPanel.add(resultsWrapper, RESULTS_PANEL);
		centerPanel.add(infoWrapper, ERROR_PANEL);

		cardLayout.show(centerPanel, ERROR_PANEL);

		container.add(slotFilter, groupConstraints);
		groupConstraints.gridy++;
		container.add(searchBar, groupConstraints);
		groupConstraints.gridy++;

		add(container, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(searchBar::requestFocusInWindow);

		swapManager.addLockChangeListener(this::updateTabIcon);
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
		PanelKitType panelSlot = Arrays.stream(PanelKitType.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.orElse(PanelKitType.ALL);
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

	private Map<PanelKitType, MaterialTab> setUpSlotFilters()
	{
		ImmutableMap.Builder<PanelKitType, MaterialTab> builder = new ImmutableMap.Builder<>();
		slotFilter.setLayout(new GridLayout(2, 5, 5, 5));
		slotFilter.setBorder(new EmptyBorder(0, 0, 10, 0));
		MaterialTab allTab = null;
		for (PanelKitType filterSlot : PanelKitType.values())
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
				isLocked = swapManager.isLocked(filterSlot.getKitType());
			}
			ImageIcon icon = iconFor(filterSlot, isLocked);
			tab.setIcon(icon);
			tab.setToolTipText(filterSlot.readableName());
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
					Integer slotId = swapManager.slotIdFor(itemComposition);
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
				updateSearchDebounced();
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
		Future<?> future = searchFuture;
		if (searchFuture != null)
		{
			future.cancel(false);
		}
		searchFuture = executor.schedule(() -> clientThread.invokeLater(() -> {
			if (!updateSearch())
			{
				// search was in progress; try again later
				updateSearchDebounced();
			}
		}), DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
	}

	// should only be called from debouncer and on client thread
	private boolean updateSearch()
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
					stats = itemManager.getItemStats(canonical, false);
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

			searchPanels.clear();
			// sort if we have to
			switch (this.sort)
			{
				case RELEASE:
					addPendingResults();
				case ALPHABETICAL:
					executor.submit(() -> {
						results.sort(itemAlphaComparator);
						addPendingResults();
					});
				case SUGGESTED:
					executor.submit(() -> {
						performSuggestedSort();
						addPendingResults();
					});
			}
		}
		return willUpdate;
	}

	private void performSuggestedSort()
	{
		colorScorer.updatePlayerInfo();
		scores = new HashMap<>();
		for (Result result : results)
		{
			int itemId = result.getItemComposition().getId();
			scores.put(itemId, colorScorer.score(itemId, selectedSlot));
		}
		results.sort(Comparator.comparing(r ->
			-scores.getOrDefault(r.getItemComposition().getId(), 0.0)));
	}

	// only to be called from updateSearch
	private void addPendingResults()
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
				for (Result result : results)
				{
					Integer itemId = result.getItemComposition().getId();
					FashionscapeSearchItemPanel panel = new FashionscapeSearchItemPanel(itemId, result.getIcon(),
						result.getSlot(), itemManager, swapManager, clientThread, listener, scores.get(itemId));
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
		});
	}

	private boolean isValidSearch(ItemComposition itemComposition, String query)
	{
		String name = itemComposition.getName().toLowerCase();
		// The client assigns "null" to item names of items it doesn't know about
		if (name.equals("null") || !name.contains(query))
		{
			return false;
		}
		return filter == null || filter.apply(itemComposition);
	}

	private void updateTabIcon(KitType slot, boolean isLocked)
	{
		Arrays.stream(PanelKitType.values())
			.filter(p -> Objects.equals(p.getKitType(), slot))
			.findFirst()
			.ifPresent(panelKitType -> updateTabIcon(panelKitType, isLocked));
	}

	private void updateTabIcon(PanelKitType panelSlot, boolean isLocked)
	{
		MaterialTab tab = tabMap.get(panelSlot);
		if (tab != null)
		{
			tab.setIcon(iconFor(panelSlot, isLocked));
		}
	}

	private ImageIcon iconFor(PanelKitType panelSlot, boolean isLocked)
	{
		String lockStr = isLocked ? "-lock" : "";
		String iconName = panelSlot.readableName().toLowerCase() + lockStr + ".png";
		return new ImageIcon(ImageUtil.loadImageResource(getClass(), iconName));
	}

}
