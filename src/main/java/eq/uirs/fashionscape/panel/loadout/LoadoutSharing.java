package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.core.loadout.LoadoutCodec;
import eq.uirs.fashionscape.core.loadout.LoadoutStore;
import eq.uirs.fashionscape.core.loadout.SavedLoadout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Copies, imports, and exports loadouts through the clipboard and files. Call on the EDT.
 */
@Slf4j
class LoadoutSharing
{
	private static final String CLIPBOARD_NAME = "Imported loadout";
	private static final String EXPORT_FILE_NAME = "loadouts.json";

	private final LoadoutCodec codec;
	private final LoadoutStore store;

	@Inject
	LoadoutSharing(LoadoutCodec codec, LoadoutStore store)
	{
		this.codec = codec;
		this.store = store;
	}

	void copy(SavedLoadout saved, Component parent)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(codec.toJson(saved.getLoadout())), null);
		showMessage(parent, "Copied \"" + saved.getLoadout().getName() + "\" to the clipboard.");
	}

	void importClipboard(Component parent)
	{
		String text;
		try
		{
			text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException | IOException e)
		{
			showError(parent, "The clipboard doesn't contain any text.");
			return;
		}
		importText(text, CLIPBOARD_NAME, parent);
	}

	void importFiles(Component parent)
	{
		List<Filepath> files = new Filepath.Chooser()
			.setIsOpen()
			.setAcceptsFiles()
			.setMultiSelectionEnabled(true)
			.setDialogTitle("Import loadouts")
			.addExtensionFilter("Loadouts (.json, .txt)", "json", "txt")
			.showDialog(parent);
		// null when canceled
		if (files == null)
		{
			return;
		}
		for (Filepath file : files)
		{
			try (BufferedReader reader = file.openBufferedReader())
			{
				String text = reader.lines().collect(Collectors.joining("\n"));
				// legacy loadouts use file as name
				importText(text, LoadoutCodec.nameFromFile(file.getFileName()), parent);
			}
			catch (IOException e)
			{
				log.warn("Could not read loadout file {}", file, e);
				showError(parent, "Could not read " + file.getFileName() + ".");
			}
		}
	}

	void exportAll(Component parent)
	{
		List<Loadout> loadouts = store.getAll().stream().map(SavedLoadout::getLoadout).collect(Collectors.toList());
		if (loadouts.isEmpty())
		{
			showError(parent, "There are no loadouts to export.");
			return;
		}
		List<Filepath> files = new Filepath.Chooser()
			.setIsSave()
			.setDialogTitle("Export loadouts")
			.addExtensionFilter("JSON (.json)", "json")
			.setDefaultExtension("json")
			.setFileName(EXPORT_FILE_NAME)
			.showDialog(parent);
		if (files == null || files.isEmpty())
		{
			return;
		}
		Filepath file = files.get(0);
		if (file.exists() && JOptionPane.showConfirmDialog(parent, file.getFileName() + " already exists. Replace it?",
			"Fashionscape", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		try
		{
			file.write(codec.toJson(loadouts));
			showMessage(parent, "Exported " + loadouts.size() + " loadouts to " + file.getFileName() + ".");
		}
		catch (IOException e)
		{
			log.warn("Could not write loadout file {}", file, e);
			showError(parent, "Could not write " + file.getFileName() + ".");
		}
	}

	private void importText(String text, String defaultName, Component parent)
	{
		try
		{
			codec.parse(text, defaultName).forEach(store::add);
		}
		catch (IllegalArgumentException e)
		{
			showError(parent, e.getMessage());
		}
	}

	private static void showMessage(Component parent, String message)
	{
		JOptionPane.showMessageDialog(parent, message, "Fashionscape", JOptionPane.INFORMATION_MESSAGE);
	}

	private static void showError(Component parent, String message)
	{
		JOptionPane.showMessageDialog(parent, message, "Fashionscape", JOptionPane.ERROR_MESSAGE);
	}
}
