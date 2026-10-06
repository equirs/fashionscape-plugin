package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.loadout.ActiveLoadouts;
import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.core.loadout.LoadoutStore;
import eq.uirs.fashionscape.core.loadout.SavedLoadout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.PluginErrorPanel;

/**
 * Lists saved loadouts. Shown in place of the item, base, and search tabs.
 */
public class LoadoutsPanel extends JPanel implements LoadoutRow.Actions
{
	private static final String DEFAULT_NAME = "Loadout ";

	private final ClientThread clientThread;
	private final FashionManager fashionManager;
	private final LoadoutStore store;
	private final ItemManager itemManager;
	private final ActiveLoadouts activeLoadouts;
	private final LoadoutSharing sharing;
	private final LoadoutsHeader header;

	private final JPanel listPanel = new JPanel(new GridBagLayout());
	private final PluginErrorPanel emptyPanel = new PluginErrorPanel();
	private final Map<String, LoadoutRow> rows = new HashMap<>();
	private boolean previewing;

	@Inject
	LoadoutsPanel(ClientThread clientThread, FashionManager fashionManager, LoadoutStore store, ItemManager itemManager,
				  ActiveLoadouts activeLoadouts, LoadoutSharing sharing, LoadoutsHeader header)
	{
		this.clientThread = clientThread;
		this.fashionManager = fashionManager;
		this.store = store;
		this.itemManager = itemManager;
		this.activeLoadouts = activeLoadouts;
		this.sharing = sharing;
		this.header = header;
		header.setOnSave(this::saveCurrent);
		header.setOnSortChanged(this::rebuild);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 0, 0, 0));
		add(header, BorderLayout.NORTH);

		emptyPanel.setContent("No saved loadouts", "Press + to save your current look.");
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel scrollWrapper = new JPanel(new BorderLayout());
		scrollWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollWrapper.setBorder(new EmptyBorder(0, 10, 5, 10));
		scrollWrapper.add(listPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setViewportView(scrollWrapper);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void setCanSave(boolean canSave)
	{
		header.setCanSave(canSave);
	}

	public void setOnClose(Runnable onClose)
	{
		header.setOnBack(onClose);
	}

	/**
	 * Recreates the rows from the store. Call on the EDT.
	 */
	public void rebuild()
	{
		// removed rows may never get a mouse exit event
		endPreview();
		listPanel.removeAll();
		rows.clear();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 5, 0);

		LoadoutSort sort = header.getSort();
		List<SavedLoadout> all = new ArrayList<>(store.getAll());
		if (sort == LoadoutSort.NAME)
		{
			// stable, so equal names keep their custom order
			all.sort(Comparator.comparing(s -> s.getLoadout().getName(), String.CASE_INSENSITIVE_ORDER));
		}
		if (all.isEmpty())
		{
			listPanel.add(emptyPanel, c);
		}
		for (int i = 0; i < all.size(); i++)
		{
			SavedLoadout saved = all.get(i);
			LoadoutColorBar colorBar = new LoadoutColorBar(saved.getLoadout());
			LoadoutRow row = new LoadoutRow(saved, summaryOf(saved.getLoadout()), colorBar, sort == LoadoutSort.CUSTOM,
				i == 0, i == all.size() - 1, this);
			row.setActive(activeLoadouts.getActiveIds().contains(saved.getId()));
			rows.put(saved.getId(), row);
			listPanel.add(row, c);
			c.gridy++;
		}
		listPanel.revalidate();
		listPanel.repaint();
	}

	/**
	 * Marks rows with these ids as active and all others as inactive. Call on the EDT.
	 */
	public void setActive(Set<String> ids)
	{
		rows.forEach((id, row) -> row.setActive(ids.contains(id)));
	}

	/**
	 * Saves the current look as a new loadout, then starts renaming it.
	 */
	private void saveCurrent()
	{
		clientThread.invokeLater(() -> {
			Loadout loadout = fashionManager.getLoadoutManager().capture().withName(nextDefaultName());
			SavedLoadout saved = store.add(loadout);
			SwingUtilities.invokeLater(() -> {
				rebuild();
				LoadoutRow row = rows.get(saved.getId());
				if (row != null)
				{
					// lay out now so the new row has a size to scroll to
					validate();
					row.scrollRectToVisible(new Rectangle(row.getSize()));
					row.startRename();
				}
			});
		});
	}

	@Override
	public void apply(SavedLoadout saved)
	{
		previewing = false;
		fashionManager.applyLoadout(saved.getLoadout());
	}

	@Override
	public void preview(SavedLoadout saved)
	{
		previewing = true;
		fashionManager.previewLoadout(saved.getLoadout());
	}

	@Override
	public void endPreview()
	{
		if (previewing)
		{
			previewing = false;
			fashionManager.endLoadoutPreview();
		}
	}

	@Override
	public void overwrite(SavedLoadout saved)
	{
		String name = saved.getLoadout().getName();
		if (confirm("Replace \"" + name + "\" with your current look?"))
		{
			clientThread.invokeLater(() ->
				store.update(saved.getId(), fashionManager.getLoadoutManager().capture().withName(name)));
		}
	}

	@Override
	public void copy(SavedLoadout saved)
	{
		sharing.copy(saved, this);
	}

	@Override
	public void rename(SavedLoadout saved, String name)
	{
		store.update(saved.getId(), saved.getLoadout().withName(name));
	}

	@Override
	public void move(SavedLoadout saved, int offset)
	{
		store.move(saved.getId(), offset);
	}

	@Override
	public void delete(SavedLoadout saved)
	{
		if (confirm("Delete \"" + saved.getLoadout().getName() + "\"?"))
		{
			store.remove(saved.getId());
		}
	}

	// item icons if there are any, otherwise kit names
	@Nullable
	private JComponent summaryOf(Loadout loadout)
	{
		List<Integer> itemIds = LoadoutIconStrip.itemIdsOf(loadout);
		if (!itemIds.isEmpty())
		{
			return new LoadoutIconStrip(itemIds, itemManager, clientThread);
		}
		List<String> kitNames = LoadoutKitNames.namesOf(loadout);
		return kitNames.isEmpty() ? null : new LoadoutKitNames(kitNames);
	}

	private boolean confirm(String message)
	{
		return JOptionPane.showConfirmDialog(this, message, "Fashionscape",
			JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	// returns the first "Loadout N" that isn't taken
	private String nextDefaultName()
	{
		Set<String> names = store.getAll().stream()
			.map(s -> s.getLoadout().getName())
			.collect(Collectors.toSet());
		int n = 1;
		while (names.contains(DEFAULT_NAME + n))
		{
			n++;
		}
		return DEFAULT_NAME + n;
	}
}
