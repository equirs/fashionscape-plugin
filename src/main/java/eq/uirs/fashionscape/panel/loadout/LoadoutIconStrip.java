package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Shows a loadout's item icons in Items tab order.
 */
class LoadoutIconStrip extends JPanel
{
	private static final int COLUMNS = 7;
	// two thirds of an item sprite (36x32)
	private static final int ICON_WIDTH = 24;
	private static final int ICON_HEIGHT = 21;

	LoadoutIconStrip(List<Integer> itemIds, ItemManager itemManager, ClientThread clientThread)
	{
		super(new GridLayout(0, COLUMNS, 1, 1));
		setOpaque(false);

		List<JLabel> itemCells = new ArrayList<>();
		for (int i = 0; i < itemIds.size(); i++)
		{
			JLabel cell = cell();
			itemCells.add(cell);
			add(cell);
		}

		if (!itemIds.isEmpty())
		{
			clientThread.invokeLater(() -> {
				for (int i = 0; i < itemIds.size(); i++)
				{
					JLabel cell = itemCells.get(i);
					int itemId = itemIds.get(i);
					String name = itemManager.getItemComposition(itemId).getMembersName();
					SwingUtilities.invokeLater(() -> cell.putClientProperty(LoadoutRow.TOOLTIP_KEY, name));
					AsyncBufferedImage image = itemManager.getImage(itemId);
					image.onLoaded(() -> SwingUtilities.invokeLater(() ->
						cell.setIcon(new ImageIcon(ImageUtil.resizeImage(image, ICON_WIDTH, ICON_HEIGHT)))));
				}
			});
		}
	}

	static List<Integer> itemIdsOf(Loadout loadout)
	{
		List<Integer> result = new ArrayList<>();
		for (PanelEquipSlot panelSlot : PanelEquipSlot.values())
		{
			KitType slot = panelSlot.getKitType();
			Integer itemId = slot != null ? loadout.getItems().get(slot) : null;
			if (itemId != null && itemId >= 0)
			{
				result.add(itemId);
			}
		}
		JawIcon icon = loadout.getIcon();
		if (icon != null && icon != JawIcon.NOTHING)
		{
			result.add(icon.getId());
		}
		return result;
	}

	private static JLabel cell()
	{
		JLabel cell = new JLabel();
		cell.setHorizontalAlignment(JLabel.CENTER);
		cell.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
		return cell;
	}
}
