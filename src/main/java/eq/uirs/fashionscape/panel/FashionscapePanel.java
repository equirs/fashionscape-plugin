package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.core.SwapManager;
import eq.uirs.fashionscape.core.event.ColorChangedListener;
import eq.uirs.fashionscape.core.event.ColorLockChangedListener;
import eq.uirs.fashionscape.core.event.IconChangedListener;
import eq.uirs.fashionscape.core.event.ItemChangedListener;
import eq.uirs.fashionscape.core.event.KitChangedListener;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import eq.uirs.fashionscape.data.color.ColorType;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class FashionscapePanel extends PluginPanel
{
	private final Client client;
	private final ClientThread clientThread;
	private final SwapManager swapManager;

	private JButton undo;
	private JButton redo;
	private JButton shuffle;
	private JButton save;
	private JButton load;
	private JButton clear;

	private final SearchClearingPanel tabDisplayPanel;
	private final MaterialTabGroup tabGroup;
	private final MaterialTab searchTab;

	private final SearchPanel searchPanel;
	private final KitsPanel kitsPanel;

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
	public FashionscapePanel(SearchPanel searchPanel, KitsPanel kitsPanel, SwapManager swapManager,
							 ItemManager itemManager, Client client, ClientThread clientThread)
	{
		super(false);
		this.client = client;
		this.clientThread = clientThread;
		this.swapManager = swapManager;
		tabDisplayPanel = new SearchClearingPanel(searchPanel);
		tabGroup = new MaterialTabGroup(tabDisplayPanel);

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
		SwapsPanel swapsPanel = new SwapsPanel(swapManager, itemManager, searchOpener, clientThread);
		this.searchPanel = searchPanel;
		this.kitsPanel = kitsPanel;

		MaterialTab swapsTab = new MaterialTab("Items", tabGroup, swapsPanel);
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
		swapsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			kitsPanel.collapseOptions();
			return true;
		});

		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(swapsTab);
		tabGroup.addTab(kitsTab);
		tabGroup.addTab(searchTab);
		tabGroup.select(swapsTab);

		JPanel buttonPanel = setUpButtonPanel();
		add(buttonPanel, BorderLayout.NORTH);

		tabPanel.add(tabGroup, BorderLayout.NORTH);
		tabPanel.add(tabDisplayPanel, BorderLayout.CENTER);
		add(tabPanel, BorderLayout.CENTER);

		// additional listeners
		final Runnable lockListener = () -> {
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			boolean unlocked = hasUnlocked();
			boolean nonEmpty = hasNonEmpty();
			checkButtonEnabled(shuffle, loggedIn, unlocked, nonEmpty);
			checkButtonEnabled(clear, loggedIn, unlocked, nonEmpty);
		};
		swapManager.addEventListener(new LockChangedListener(e -> lockListener.run()));
		swapManager.addEventListener(new ColorLockChangedListener(e -> lockListener.run()));
		final Runnable saveClearListener = () -> {
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			boolean unlocked = hasUnlocked();
			boolean nonEmpty = hasNonEmpty();
			checkButtonEnabled(save, loggedIn, unlocked, nonEmpty);
			checkButtonEnabled(clear, loggedIn, unlocked, nonEmpty);
		};
		swapManager.addEventListener(new ItemChangedListener(e -> saveClearListener.run()));
		swapManager.addEventListener(new KitChangedListener(e -> saveClearListener.run()));
		swapManager.addEventListener(new ColorChangedListener(e -> saveClearListener.run()));
		swapManager.addEventListener(new IconChangedListener(e -> saveClearListener.run()));
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		boolean loggedIn = event.getGameState() == GameState.LOGGED_IN;
		boolean hasNonEmpty = hasNonEmpty();
		boolean hasUnlocked = hasUnlocked();
		checkButtonEnabled(undo, loggedIn, hasUnlocked, hasNonEmpty);
		checkButtonEnabled(redo, loggedIn, hasUnlocked, hasNonEmpty);
		checkButtonEnabled(shuffle, loggedIn, hasUnlocked, hasNonEmpty);
		checkButtonEnabled(save, loggedIn, hasUnlocked, hasNonEmpty);
		checkButtonEnabled(load, loggedIn, hasUnlocked, hasNonEmpty);
		checkButtonEnabled(clear, loggedIn, hasUnlocked, hasNonEmpty);
	}

	public void onPlayerChanged(Player player)
	{
		if (kitsPanel != null)
		{
			kitsPanel.onPlayerChanged(player);
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

		boolean isLoggedIn = client.getGameState() == GameState.LOGGED_IN;
		boolean hasUnlocked = hasUnlocked();
		boolean hasNonEmpty = hasNonEmpty();

		undo = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "undo.png")));
		undo.setToolTipText("Undo");
		undo.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.undoLastSwap();
			reloadResults();
		}));
		checkButtonEnabled(undo, isLoggedIn, hasUnlocked, hasNonEmpty);
		undo.addMouseListener(createHoverListener(undo));
		swapManager.addUndoQueueChangeListener(size -> checkButtonEnabled(undo, null, null, null));
		undo.setFocusPainted(false);
		buttonContainer.add(undo, c);
		c.gridx++;

		redo = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "redo.png")));
		redo.setToolTipText("Redo");
		redo.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.redoLastSwap();
			reloadResults();
		}));
		checkButtonEnabled(redo, isLoggedIn, hasUnlocked, hasNonEmpty);
		redo.addMouseListener(createHoverListener(redo));
		swapManager.addRedoQueueChangeListener(size -> checkButtonEnabled(redo, null, null, null));
		redo.setFocusPainted(false);
		buttonContainer.add(redo, c);
		c.gridx++;

		shuffle = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "shuffle.png")));
		shuffle.setSize(12, 12);
		shuffle.setToolTipText("Randomize");
		shuffle.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.shuffle();
			reloadResults();
		}));
		checkButtonEnabled(shuffle, isLoggedIn, hasUnlocked, hasNonEmpty);
		shuffle.setFocusPainted(false);
		shuffle.addMouseListener(createHoverListener(shuffle));
		buttonContainer.add(shuffle, c);
		c.gridx++;

		JPopupMenu openSavedFolderMenu = new JPopupMenu();
		JMenuItem openAll = new JMenuItem("Open outfits folder");
		openAll.addActionListener(e -> LinkBrowser.open(FashionscapePlugin.OUTFITS_DIR.toString()));
		openSavedFolderMenu.add(openAll);

		save = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "save.png")));
		save.setToolTipText("Save");
		save.addActionListener(e -> openSaveDialog());
		save.setFocusPainted(false);
		save.addMouseListener(createHoverListener(save));
		save.setComponentPopupMenu(openSavedFolderMenu);
		checkButtonEnabled(save, isLoggedIn, hasUnlocked, hasNonEmpty);
		buttonContainer.add(save, c);
		c.gridx++;

		load = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "load.png")));
		load.setToolTipText("Load");
		load.addActionListener(e -> openLoadDialog());
		load.setFocusPainted(false);
		load.setComponentPopupMenu(openSavedFolderMenu);
		checkButtonEnabled(load, isLoggedIn, hasUnlocked, hasNonEmpty);
		load.addMouseListener(createHoverListener(load));
		buttonContainer.add(load, c);
		c.gridx++;

		JPopupMenu softClearMenu = new JPopupMenu();
		JMenuItem softClear = new JMenuItem("Soft clear");
		softClear.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revertSwaps(false, false);
			reloadResults();
		}));
		softClearMenu.add(softClear);

		clear = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "clear.png")));
		clear.setToolTipText("Clear all");
		clear.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revertSwaps(true, false);
			reloadResults();
		}));
		clear.setFocusPainted(false);
		clear.addMouseListener(createHoverListener(clear));
		clear.setComponentPopupMenu(softClearMenu);
		checkButtonEnabled(clear, isLoggedIn, hasUnlocked, hasNonEmpty);
		buttonContainer.add(clear, c);

		return buttonContainer;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void openSaveDialog()
	{
		File outputDir = FashionscapePlugin.OUTFITS_DIR;
		outputDir.mkdirs();

		JFileChooser fileChooser = new JFileChooser(outputDir)
		{
			@Override
			public void approveSelection()
			{
				File f = getSelectedFile();
				if (!f.getName().endsWith(".txt"))
				{
					f = new File(f.getPath() + ".txt");
				}
				if (f.exists() && getDialogType() == SAVE_DIALOG)
				{
					int result = JOptionPane.showConfirmDialog(
						this,
						"File already exists, overwrite?",
						"Warning",
						JOptionPane.YES_NO_CANCEL_OPTION
					);
					switch (result)
					{
						case JOptionPane.YES_OPTION:
							super.approveSelection();
							return;
						case JOptionPane.NO_OPTION:
						case JOptionPane.CLOSED_OPTION:
							return;
						case JOptionPane.CANCEL_OPTION:
							cancelSelection();
							return;
					}
				}
				super.approveSelection();
			}
		};
		fileChooser.setSelectedFile(new File("outfit.txt"));
		fileChooser.setDialogTitle("Save current outfit");

		int option = fileChooser.showSaveDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			if (!selectedFile.getName().endsWith(".txt"))
			{
				selectedFile = new File(selectedFile.getPath() + ".txt");
			}
			swapManager.exportSwaps(selectedFile);
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void openLoadDialog()
	{
		File outputDir = FashionscapePlugin.OUTFITS_DIR;
		outputDir.mkdirs();

		JFileChooser fileChooser = new JFileChooser(outputDir);
		fileChooser.setDialogTitle("Select an outfit to load");

		int option = fileChooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile)))
			{
				List<String> lines = reader.lines().collect(Collectors.toList());
				swapManager.loadImports(lines);
			}
			catch (IOException e)
			{
				log.warn("Failed to import swaps from file", e);
			}
		}
	}

	private MouseAdapter createHoverListener(JButton jButton)
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (jButton.isEnabled())
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		};
	}

	private boolean hasNonEmpty()
	{
		long numSlotSwaps = Arrays.stream(KitType.values())
			.filter(slot -> {
				Integer item = swapManager.swappedItemIdIn(slot);
				Integer kit = swapManager.swappedKitIdIn(slot);
				boolean containsNothing = swapManager.isHidden(slot);
				return item != null || kit != null || containsNothing;
			})
			.count();
		long numColorSwaps = Arrays.stream(ColorType.values())
			.map(swapManager::swappedColorIdIn)
			.filter(Objects::nonNull)
			.count();
		boolean hasIcon = swapManager.swappedIcon() != null;
		return numSlotSwaps + numColorSwaps > 0 || hasIcon;
	}

	private boolean hasUnlocked()
	{
		long numUnlockedSlots = Arrays.stream(KitType.values())
			.filter(Objects::nonNull)
			.map(swapManager::isSlotLocked)
			.filter(b -> !b)
			.count();
		long numUnlockedColors = Arrays.stream(ColorType.values())
			.filter(Objects::nonNull)
			.map(swapManager::isColorLocked)
			.filter(b -> !b)
			.count();
		long unlockedIcon = swapManager.isIconLocked() ? 0 : 1;
		return numUnlockedSlots + numUnlockedColors + unlockedIcon > 0;
	}

	private void checkButtonEnabled(JButton button, Boolean isLoggedIn, Boolean hasUnlocked, Boolean hasNonEmpty)
	{
		if (button == null)
		{
			return;
		}
		boolean loggedIn = isLoggedIn != null ? isLoggedIn : client.getGameState() == GameState.LOGGED_IN;
		boolean unlocked = hasUnlocked != null ? hasUnlocked : hasUnlocked();
		boolean nonEmpty = hasNonEmpty != null ? hasNonEmpty : hasNonEmpty();
		boolean enabled = loggedIn;
		if (button == undo)
		{
			enabled &= swapManager.canUndo();
		}
		else if (button == redo)
		{
			enabled &= swapManager.canRedo();
		}
		else if (button == shuffle)
		{
			enabled &= unlocked;
		}
		else if (button == save)
		{
			enabled &= nonEmpty;
		}
		else if (button == clear)
		{
			// can clear regardless of login state
			enabled = nonEmpty;
		}
		// note: load just requires login
		button.setEnabled(enabled);
	}
}

