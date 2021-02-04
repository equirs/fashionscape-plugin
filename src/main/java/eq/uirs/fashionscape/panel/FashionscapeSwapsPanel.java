package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class FashionscapeSwapsPanel extends JPanel
{
	private final SwapManager swapManager;
	private final ItemManager itemManager;
	private final Client client;
	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final SearchOpener searchOpener;
	private final List<FashionscapeSwapItemPanel> itemPanels = new ArrayList<>();

	private JPanel slotsPanel;
	private JButton undo;
	private JButton redo;
	private JButton shuffle;
	private JButton save;
	private JButton load;
	private JButton clear;

	@Value
	private static class SlotResult
	{
		KitType slot;
		Integer itemId;
		BufferedImage image;
	}

	public FashionscapeSwapsPanel(SwapManager swapManager, ItemManager itemManager, Client client,
								  ChatMessageManager chatMessageManager, SearchOpener searchOpener,
								  ClientThread clientThread)
	{
		this.swapManager = swapManager;
		this.itemManager = itemManager;
		this.chatMessageManager = chatMessageManager;
		this.searchOpener = searchOpener;
		this.client = client;
		this.clientThread = clientThread;

		setLayout(new GridBagLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		JPanel buttonPanel = setUpButtonPanel();
		add(buttonPanel, c);
		c.gridy++;
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

	private JPanel setUpButtonPanel()
	{
		JPanel buttonContainer = new JPanel();
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
		undo.addActionListener(e -> swapManager.undoLastSwap());
		checkButtonEnabled(undo, isLoggedIn, hasUnlocked, hasNonEmpty);
		undo.addMouseListener(createHoverListener(undo));
		swapManager.addUndoQueueChangeListener(size -> checkButtonEnabled(undo, null, null, null));
		undo.setFocusPainted(false);
		buttonContainer.add(undo, c);
		c.gridx++;

		redo = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "redo.png")));
		redo.setToolTipText("Redo");
		redo.addActionListener(e -> swapManager.redoLastSwap());
		checkButtonEnabled(redo, isLoggedIn, hasUnlocked, hasNonEmpty);
		redo.addMouseListener(createHoverListener(redo));
		swapManager.addRedoQueueChangeListener(size -> checkButtonEnabled(redo, null, null, null));
		redo.setFocusPainted(false);
		buttonContainer.add(redo, c);
		c.gridx++;

		shuffle = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "shuffle.png")));
		shuffle.setSize(12, 12);
		shuffle.setToolTipText("Randomize");
		shuffle.addActionListener(e -> swapManager.shuffle());
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

		JPopupMenu forceClearMenu = new JPopupMenu();
		JMenuItem forceClear = new JMenuItem("Force clear");
		forceClear.addActionListener(e -> {
			swapManager.revertSwaps(true);
			for (FashionscapeSwapItemPanel itemPanel : itemPanels)
			{
				itemPanel.updateLockButton();
			}
		});
		forceClearMenu.add(forceClear);

		clear = new JButton(new ImageIcon(ImageUtil.loadImageResource(getClass(), "clear.png")));
		clear.setToolTipText("Clear all");
		clear.addActionListener(e -> swapManager.revertSwaps(false));
		clear.setFocusPainted(false);
		clear.addMouseListener(createHoverListener(clear));
		clear.setComponentPopupMenu(forceClearMenu);
		checkButtonEnabled(clear, isLoggedIn, hasUnlocked, hasNonEmpty);
		buttonContainer.add(clear, c);

		// Add slots directly under buttons (kinda hacky but w/e, aligning panels sucks)
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 6;

		slotsPanel = new JPanel();
		slotsPanel.setLayout(new GridBagLayout());
		slotsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		populateSwapSlots();
		buttonContainer.add(slotsPanel, c);

		swapManager.addLockChangeListener((slot, locked) -> {
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			boolean unlocked = hasUnlocked();
			boolean nonEmpty = hasNonEmpty();
			checkButtonEnabled(shuffle, loggedIn, unlocked, nonEmpty);
			checkButtonEnabled(clear, loggedIn, unlocked, nonEmpty);
		});
		swapManager.addItemChangeListener((slot, itemId) -> {
			boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
			boolean unlocked = hasUnlocked();
			boolean nonEmpty = hasNonEmpty();
			checkButtonEnabled(save, loggedIn, unlocked, nonEmpty);
			checkButtonEnabled(clear, loggedIn, unlocked, nonEmpty);
		});

		return buttonContainer;
	}

	private void populateSwapSlots()
	{
		clientThread.invokeLater(() -> {
			List<SlotResult> slotResults = new ArrayList<>();
			for (PanelKitType panelSlot : PanelKitType.values())
			{
				KitType slot = panelSlot.getKitType();
				if (slot == null)
				{
					continue;
				}
				Integer itemId = swapManager.swappedItemIdIn(slot);
				BufferedImage image;
				if (itemId != null)
				{
					ItemComposition itemComposition = itemManager.getItemComposition(itemId);
					image = itemManager.getImage(itemComposition.getId());
				}
				else
				{
					image = ImageUtil.loadImageResource(getClass(), slot.name().toLowerCase() + ".png");
				}
				slotResults.add(new SlotResult(slot, itemId, image));
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
			FashionscapeSwapItemPanel itemPanel = new FashionscapeSwapItemPanel(s.itemId, s.image, itemManager,
				clientThread, swapManager, s.slot, searchOpener);
			itemPanels.add(itemPanel);
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(itemPanel, BorderLayout.NORTH);
			slotsPanel.add(marginWrapper, c);
			c.gridy++;
		}
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
			File finalSelectedFile = selectedFile;
			clientThread.invokeLater(() -> {
				try (PrintWriter out = new PrintWriter(finalSelectedFile))
				{
					List<String> exports = swapManager.stringifySwaps();
					for (String line : exports)
					{
						out.println(line);
					}
					sendHighlightedMessage("Outfit saved to " + finalSelectedFile.getName());
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			});
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
				loadImports(lines);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void loadImports(List<String> allLines)
	{
		Map<KitType, Integer> toImport = new HashMap<>();
		for (String line : allLines)
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			Matcher matcher = FashionscapePlugin.PROFILE_PATTERN.matcher(line);
			if (matcher.matches())
			{
				String slotStr = matcher.group(1);
				int itemId = Integer.parseInt(matcher.group(2));
				KitType slot = stringMatch(slotStr);
				if (slot != null)
				{
					toImport.put(slot, itemId);
				}
				else
				{
					sendHighlightedMessage("Could not process line: " + line);
				}
			}
		}
		if (!toImport.isEmpty())
		{
			swapManager.importSwaps(toImport);
		}
	}

	private void sendHighlightedMessage(String message)
	{
		String chatMessage = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(message)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(chatMessage)
			.build());
	}

	@Nullable
	private KitType stringMatch(String name)
	{
		try
		{
			return KitType.valueOf(name);
		}
		catch (Exception e)
		{
			return null;
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
		long numNonEmpty = Arrays.stream(PanelKitType.values())
			.map(PanelKitType::getKitType)
			.filter(Objects::nonNull)
			.map(swapManager::swappedItemIdIn)
			.filter(Objects::nonNull)
			.count();
		return numNonEmpty > 0;
	}

	private boolean hasUnlocked()
	{
		long numUnlocked = Arrays.stream(PanelKitType.values())
			.map(PanelKitType::getKitType)
			.filter(Objects::nonNull)
			.map(swapManager::isLocked)
			.filter(b -> !b)
			.count();
		return numUnlocked > 0;
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
			enabled = nonEmpty && unlocked;
		}
		// note: load just requires login
		button.setEnabled(enabled);
	}
}
