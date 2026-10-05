package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.ActiveLoadoutsChanged;
import eq.uirs.fashionscape.core.event.ColorChanged;
import eq.uirs.fashionscape.core.event.ColorLockChanged;
import eq.uirs.fashionscape.core.event.HistoryChanged;
import eq.uirs.fashionscape.core.event.IconChanged;
import eq.uirs.fashionscape.core.event.IconLockChanged;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.KitChanged;
import eq.uirs.fashionscape.core.event.KnownKitChanged;
import eq.uirs.fashionscape.core.event.LoadoutsChanged;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.panel.loadout.LoadoutsPanel;
import eq.uirs.fashionscape.remote.RemoteCategory;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

@Slf4j
public class FashionscapePanel extends PluginPanel
{
	private final ClientThread clientThread;
	private final FashionManager fashionManager;
	private final RemoteDataHandler remote;

	private JButton undo;
	private JButton redo;
	private JButton shuffle;
	private JButton save;
	private JButton clear;

	private final SearchClearingPanel tabDisplayPanel;
	private final MaterialTabGroup tabGroup;
	private final MaterialTab searchTab;

	private final SearchPanel searchPanel;
	private final KitsPanel kitsPanel;
	private final ItemsPanel itemsPanel;
	private final LoadoutsPanel loadoutsPanel;
	private final NetworkErrorPanel networkErrorPanel;

	private static final String TABS_CARD = "tabs";
	private static final String LOADOUTS_CARD = "loadouts";
	private final JPanel cards = new JPanel(new CardLayout());
	private boolean showingLoadouts;

	@RequiredArgsConstructor
	static class SearchClearingPanel extends JPanel
	{
		boolean shouldClearSearch = true;
		private final SearchPanel searchPanel;

		@Override
		public void removeAll()
		{
			if (shouldClearSearch)
			{
				searchPanel.clearResults();
			}
			super.removeAll();
		}
	}

	@Inject
	public FashionscapePanel(SearchPanel searchPanel, KitsPanel kitsPanel, DebugAnimationsPanel animsPanel,
							 LoadoutsPanel loadoutsPanel, FashionManager fashionManager,
							 ItemManager itemManager, ClientThread clientThread,
							 RemoteDataHandler remote, @Named("developerMode") boolean developerMode)
	{
		super(false);
		this.clientThread = clientThread;
		this.fashionManager = fashionManager;
		this.remote = remote;
		tabDisplayPanel = new SearchClearingPanel(searchPanel);
		tabGroup = new MaterialTabGroup(tabDisplayPanel);
		networkErrorPanel = new NetworkErrorPanel();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel tabPanel = new JPanel();
		tabPanel.setLayout(new BorderLayout());
		tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		SearchOpener searchOpener = new SearchOpener()
		{
			@Override
			public void openSearchFor(KitType slot)
			{
				tabGroup.select(searchTab);
				searchPanel.chooseSlot(slot);
				searchPanel.clearSearch();
			}
		};
		ItemsPanel itemsPanel = new ItemsPanel(fashionManager, itemManager, searchOpener, clientThread, developerMode);
		this.itemsPanel = itemsPanel;
		this.searchPanel = searchPanel;
		this.kitsPanel = kitsPanel;
		this.loadoutsPanel = loadoutsPanel;
		loadoutsPanel.setOnClose(this::closeLoadouts);
		loadoutsPanel.setCanSave(hasVirtuals());

		MaterialTab itemsTab = new MaterialTab("Items", tabGroup, itemsPanel);
		MaterialTab kitsTab = new MaterialTab("Base", tabGroup, kitsPanel);
		searchTab = new MaterialTab("Search", tabGroup, searchPanel);

		// need some hacky listeners set up to clear search results when changing tabs
		searchTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = false;
			searchPanel.reloadResults();
			kitsPanel.collapseOptions();
			return true;
		});
		kitsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			return true;
		});
		itemsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			kitsPanel.collapseOptions();
			return true;
		});

		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(itemsTab);
		tabGroup.addTab(kitsTab);
		tabGroup.addTab(searchTab);
		if (developerMode)
		{
			// there's barely room for this tab; if the label is longer, the tab won't add
			MaterialTab animsTab = new MaterialTab("Dev", tabGroup, animsPanel);
			animsTab.setOnSelectEvent(() -> {
				tabDisplayPanel.shouldClearSearch = true;
				return true;
			});
			tabGroup.addTab(animsTab);
		}
		tabGroup.select(itemsTab);

		JPanel buttonPanel = setUpButtonPanel();
		add(buttonPanel, BorderLayout.NORTH);

		tabPanel.add(tabGroup, BorderLayout.NORTH);
		tabPanel.add(tabDisplayPanel, BorderLayout.CENTER);
		cards.add(tabPanel, TABS_CARD);
		cards.add(loadoutsPanel, LOADOUTS_CARD);
		add(cards, BorderLayout.CENTER);

		if (remote.hasFailed())
		{
			add(networkErrorPanel, BorderLayout.SOUTH);
		}

		remote.addOnReceiveDataListener(this::onExternalFetchFinished);
	}

	@Subscribe
	public void onLockChanged(LockChanged e)
	{
		refreshButtonsEnabled();
		itemsPanel.onLockChanged(e);
		searchPanel.onLockChanged(e);
		kitsPanel.onLockChanged(e);
	}

	@Subscribe
	public void onIconLockChanged(IconLockChanged e)
	{
		kitsPanel.onIconLockChanged(e);
	}

	@Subscribe
	public void onColorLockChanged(ColorLockChanged e)
	{
		refreshButtonsEnabled();
		kitsPanel.onColorLockChanged(e);
	}

	@Subscribe
	public void onItemChanged(ItemChanged e)
	{
		log.debug("item changed {}", e);
		refreshButtonsEnabled();
		itemsPanel.onItemChanged(e);
		kitsPanel.onItemChanged(e);
	}

	@Subscribe
	public void onKitChanged(KitChanged e)
	{
		refreshButtonsEnabled();
		kitsPanel.onKitChanged(e);
	}

	@Subscribe
	public void onColorChanged(ColorChanged e)
	{
		refreshButtonsEnabled();
		kitsPanel.onColorChanged(e);
	}

	@Subscribe
	public void onIconChanged(IconChanged e)
	{
		refreshButtonsEnabled();
		kitsPanel.onIconChanged(e);
	}

	@Subscribe
	public void onHistoryChanged(HistoryChanged e)
	{
		checkButtonEnabled(e.isUndo() ? undo : redo);
	}

	@Subscribe
	public void onKnownKitChanged(KnownKitChanged e)
	{
		itemsPanel.onKnownKitChanged(e);
	}

	@Subscribe
	public void onLoadoutsChanged(LoadoutsChanged e)
	{
		SwingUtilities.invokeLater(loadoutsPanel::rebuild);
	}

	@Subscribe
	public void onActiveLoadoutsChanged(ActiveLoadoutsChanged e)
	{
		SwingUtilities.invokeLater(() -> loadoutsPanel.setActive(e.getIds()));
	}

	private void refreshButtonsEnabled()
	{
		checkButtonEnabled(shuffle);
		checkButtonEnabled(clear);
		checkButtonEnabled(save);
		loadoutsPanel.setCanSave(hasVirtuals());
	}

	public void onPlayerChanged(Player player)
	{
		if (kitsPanel != null)
		{
			kitsPanel.onPlayerChanged(player);
		}
		if (itemsPanel != null)
		{
			itemsPanel.refreshWeaponWarnings();
		}
	}

	public void reloadResults()
	{
		if (searchPanel != null)
		{
			searchPanel.reloadResults();
		}
	}

	public void refreshKitsPanel()
	{
		if (kitsPanel != null)
		{
			kitsPanel.populateKitSlots();
		}
	}

	private JPanel setUpButtonPanel()
	{
		JPanel buttonContainer = new JPanel();
		buttonContainer.setBorder(new EmptyBorder(5, 10, 0, 10));
		buttonContainer.setLayout(new GridBagLayout());
		buttonContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 1, 0, 1);
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		undo = new JButton(PanelUtil.icon("undo"));
		undo.setToolTipText("Undo");
		undo.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.undo();
			reloadResults();
		}));
		checkButtonEnabled(undo);
		undo.addMouseListener(PanelUtil.hoverCursor(this));
		undo.setFocusPainted(false);
		buttonContainer.add(undo, c);
		c.gridx++;

		redo = new JButton(PanelUtil.icon("redo"));
		redo.setToolTipText("Redo");
		redo.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.redo();
			reloadResults();
		}));
		checkButtonEnabled(redo);
		redo.addMouseListener(PanelUtil.hoverCursor(this));
		redo.setFocusPainted(false);
		buttonContainer.add(redo, c);
		c.gridx++;

		shuffle = new JButton(PanelUtil.icon("shuffle"));
		shuffle.setSize(12, 12);
		shuffle.setToolTipText("Randomize");
		shuffle.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.shuffle();
			reloadResults();
		}));
		checkButtonEnabled(shuffle);
		shuffle.setFocusPainted(false);
		shuffle.addMouseListener(PanelUtil.hoverCursor(this));
		buttonContainer.add(shuffle, c);
		c.gridx++;

		save = new JButton(PanelUtil.icon("save"));
		save.setToolTipText("Save as loadout");
		save.addActionListener(e -> {
			openLoadouts();
			loadoutsPanel.saveCurrent();
		});
		save.setFocusPainted(false);
		save.addMouseListener(PanelUtil.hoverCursor(this));
		checkButtonEnabled(save);
		buttonContainer.add(save, c);
		c.gridx++;

		JButton load = new JButton(PanelUtil.icon("load"));
		load.setToolTipText("Loadouts");
		load.addActionListener(e -> {
			if (showingLoadouts)
			{
				closeLoadouts();
			}
			else
			{
				openLoadouts();
			}
		});
		load.setFocusPainted(false);
		checkButtonEnabled(load);
		load.addMouseListener(PanelUtil.hoverCursor(this));
		buttonContainer.add(load, c);
		c.gridx++;

		JPopupMenu softClearMenu = new JPopupMenu();
		JMenuItem softClear = new JMenuItem("Soft clear");
		softClear.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.clear(false);
			reloadResults();
		}));
		softClearMenu.add(softClear);

		clear = new JButton(PanelUtil.icon("clear"));
		clear.setToolTipText("Clear all");
		clear.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.clear(true);
			reloadResults();
		}));
		clear.setFocusPainted(false);
		clear.addMouseListener(PanelUtil.hoverCursor(this));
		clear.setComponentPopupMenu(softClearMenu);
		checkButtonEnabled(clear);
		buttonContainer.add(clear, c);

		return buttonContainer;
	}

	private void openLoadouts()
	{
		showingLoadouts = true;
		loadoutsPanel.rebuild();
		((CardLayout) cards.getLayout()).show(cards, LOADOUTS_CARD);
	}

	private void closeLoadouts()
	{
		showingLoadouts = false;
		((CardLayout) cards.getLayout()).show(cards, TABS_CARD);
		// results may be stale after applying a loadout
		clientThread.invokeLater(this::reloadResults);
	}

	private boolean hasVirtuals()
	{
		return !fashionManager.getLayers().getVirtualModels().isEmpty();
	}

	private boolean hasUnlocked()
	{
		long numUnlockedSlots = Arrays.stream(KitType.values())
			.filter(Objects::nonNull)
			.map(fashionManager::isSlotLocked)
			.filter(b -> !b)
			.count();
		long numUnlockedColors = Arrays.stream(ColorType.values())
			.filter(Objects::nonNull)
			.map(fashionManager::isColorLocked)
			.filter(b -> !b)
			.count();
		long unlockedIcon = fashionManager.isIconLocked() ? 0 : 1;
		return numUnlockedSlots + numUnlockedColors + unlockedIcon > 0;
	}

	private void checkButtonEnabled(JButton button)
	{
		if (button == null)
		{
			return;
		}
		boolean enabled = true;
		if (button == undo)
		{
			enabled = fashionManager.canUndo();
		}
		else if (button == redo)
		{
			enabled = fashionManager.canRedo();
		}
		else if (button == shuffle)
		{
			enabled = hasUnlocked();
		}
		else if (button == save)
		{
			enabled = hasVirtuals();
		}
		// other buttons are always enabled
		button.setEnabled(enabled);
	}

	private void onExternalFetchFinished(RemoteCategory category, boolean hasFailed)
	{
		remove(networkErrorPanel);
		if (remote.hasFailed())
		{
			add(networkErrorPanel, BorderLayout.SOUTH);
		}
	}
}

