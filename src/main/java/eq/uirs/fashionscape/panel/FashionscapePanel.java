package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.Exporter;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.ColorChangedListener;
import eq.uirs.fashionscape.core.event.ColorLockChangedListener;
import eq.uirs.fashionscape.core.event.HistoryChangedListener;
import eq.uirs.fashionscape.core.event.IconChangedListener;
import eq.uirs.fashionscape.core.event.ItemChangedListener;
import eq.uirs.fashionscape.core.event.KitChangedListener;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.remote.RemoteCategory;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
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
import javax.inject.Named;
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
import net.runelite.api.Player;
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
	private final NetworkErrorPanel networkErrorPanel;

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
							 FashionManager fashionManager, ItemManager itemManager, ClientThread clientThread,
							 RemoteDataHandler remote, @Named("developerMode") boolean developerMode)
	{
		super(false);
		this.clientThread = clientThread;
		this.fashionManager = fashionManager;
		this.remote = remote;
		tabDisplayPanel = new SearchClearingPanel(searchPanel);
		tabGroup = new MaterialTabGroup(tabDisplayPanel);
		networkErrorPanel = new NetworkErrorPanel(remote);

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
		this.searchPanel = searchPanel;
		this.kitsPanel = kitsPanel;

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
		add(tabPanel, BorderLayout.CENTER);

		if (remote.hasFailed())
		{
			add(networkErrorPanel, BorderLayout.SOUTH);
		}

		// additional listeners
		final Runnable lockListener = () -> {
			checkButtonEnabled(shuffle);
			checkButtonEnabled(clear);
		};
		Events.addListener(new LockChangedListener(e -> lockListener.run()));
		Events.addListener(new ColorLockChangedListener(e -> lockListener.run()));
		final Runnable saveClearListener = () -> {
			checkButtonEnabled(save);
			checkButtonEnabled(clear);
		};
		Events.addListener(new ItemChangedListener(e -> saveClearListener.run()));
		Events.addListener(new KitChangedListener(e -> saveClearListener.run()));
		Events.addListener(new ColorChangedListener(e -> saveClearListener.run()));
		Events.addListener(new IconChangedListener(e -> saveClearListener.run()));
		remote.addOnReceiveDataListener(this::onExternalFetchFinished);
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

		undo = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "undo.png")));
		undo.setToolTipText("Undo");
		undo.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.undo();
			reloadResults();
		}));
		checkButtonEnabled(undo);
		undo.addMouseListener(createHoverListener(undo));
		undo.setFocusPainted(false);
		buttonContainer.add(undo, c);
		c.gridx++;

		redo = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "redo.png")));
		redo.setToolTipText("Redo");
		redo.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.redo();
			reloadResults();
		}));
		checkButtonEnabled(redo);
		redo.addMouseListener(createHoverListener(redo));
		redo.setFocusPainted(false);
		buttonContainer.add(redo, c);
		c.gridx++;

		Events.addListener(new HistoryChangedListener(event ->
			checkButtonEnabled(event.isUndo() ? undo : redo)));

		shuffle = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "shuffle.png")));
		shuffle.setSize(12, 12);
		shuffle.setToolTipText("Randomize");
		shuffle.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.shuffle();
			reloadResults();
		}));
		checkButtonEnabled(shuffle);
		shuffle.setFocusPainted(false);
		shuffle.addMouseListener(createHoverListener(shuffle));
		buttonContainer.add(shuffle, c);
		c.gridx++;

		JPopupMenu openSavedFolderMenu = new JPopupMenu();
		JMenuItem openAll = new JMenuItem("Open outfits folder");
		openAll.addActionListener(e -> LinkBrowser.open(Exporter.OUTFITS_DIR.toString()));
		openSavedFolderMenu.add(openAll);

		save = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "save.png")));
		save.setToolTipText("Save");
		save.addActionListener(e -> openSaveDialog());
		save.setFocusPainted(false);
		save.addMouseListener(createHoverListener(save));
		save.setComponentPopupMenu(openSavedFolderMenu);
		checkButtonEnabled(save);
		buttonContainer.add(save, c);
		c.gridx++;

		JPopupMenu cloneSelfMenu = new JPopupMenu();
		JMenuItem cloneSelf = new JMenuItem("Load current equipment");
		cloneSelf.addActionListener(e -> fashionManager.importSelf());
		cloneSelfMenu.add(cloneSelf);

		JButton load = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "load.png")));
		load.setToolTipText("Load");
		load.addActionListener(e -> openLoadDialog());
		load.setFocusPainted(false);
		load.setComponentPopupMenu(cloneSelfMenu);
		checkButtonEnabled(load);
		load.addMouseListener(createHoverListener(load));
		buttonContainer.add(load, c);
		c.gridx++;

		JPopupMenu softClearMenu = new JPopupMenu();
		JMenuItem softClear = new JMenuItem("Soft clear");
		softClear.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.clear(false);
			reloadResults();
		}));
		softClearMenu.add(softClear);

		clear = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "clear.png")));
		clear.setToolTipText("Clear all");
		clear.addActionListener(e -> clientThread.invokeLater(() -> {
			fashionManager.clear(true);
			reloadResults();
		}));
		clear.setFocusPainted(false);
		clear.addMouseListener(createHoverListener(clear));
		clear.setComponentPopupMenu(softClearMenu);
		checkButtonEnabled(clear);
		buttonContainer.add(clear, c);

		return buttonContainer;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void openSaveDialog()
	{
		File outputDir = Exporter.OUTFITS_DIR;
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
			fashionManager.getExporter().export(selectedFile);
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void openLoadDialog()
	{
		File outputDir = Exporter.OUTFITS_DIR;
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
				fashionManager.getExporter().parseImports(lines);
				clientThread.invokeLater(fashionManager::refreshPlayer);
			}
			catch (IOException e)
			{
				log.warn("Failed to import fashionscape from file", e);
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

	private boolean hasVirtuals()
	{
		long numVirtualSlots = Arrays.stream(KitType.values())
			.filter(slot -> fashionManager.getLayers().getVirtualModels().contains(slot))
			.count();
		long numVirtualColors = Arrays.stream(ColorType.values())
			.map(fashionManager::virtualColorIdFor)
			.filter(Objects::nonNull)
			.count();
		boolean hasIcon = fashionManager.virtualIcon() != null;
		return numVirtualSlots + numVirtualColors > 0 || hasIcon;
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

