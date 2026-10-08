package eq.uirs.fashionscape.panel;

import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import okhttp3.HttpUrl;

@Slf4j
abstract class AbsItemPanel extends AbsIconLabelPanel
{
	private static final HttpUrl WIKI_LOOKUP = HttpUrl.get("https://oldschool.runescape.wiki/w/Special:Lookup");

	protected final ItemManager itemManager;
	private final boolean developerMode;

	AbsItemPanel(BufferedImage image, ItemManager itemManager, ClientThread clientThread, boolean developerMode)
	{
		this(image, itemManager, clientThread, developerMode, new JLabel());
	}

	AbsItemPanel(BufferedImage image, ItemManager itemManager, ClientThread clientThread, boolean developerMode,
				 JLabel label)
	{
		super(image, clientThread, label);
		this.itemManager = itemManager;
		this.developerMode = developerMode;
		label.setInheritsPopupMenu(true);
	}

	protected void setItemName(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			String itemName = "Not set";
			if (itemId != null)
			{
				if (itemId >= 0)
				{
					// this can be called very early, before client is able to get item compositions
					try
					{
						ItemComposition itemComposition = itemManager.getItemComposition(itemId);
						itemName = itemComposition.getMembersName();
					}
					catch (Exception e)
					{
						return false;
					}
				}
				else
				{
					itemName = NothingItemComposition.NAME;
				}
			}
			label.setText(itemName);
			label.setToolTipText(itemId != null && developerMode ? "item id " + itemId : null);
			return true;
		});
	}

	// shows an "open wiki" right-click menu for items
	protected void setWikiMenu(Integer itemId)
	{
		if (itemId == null || itemId < 0)
		{
			setComponentPopupMenu(null);
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem openWiki = new JMenuItem("Open wiki");
		openWiki.addActionListener(e -> clientThread.invokeLater(() -> openWiki(itemId)));
		menu.add(openWiki);
		setComponentPopupMenu(menu);
	}

	private void openWiki(int itemId)
	{
		int canonicalId = itemManager.canonicalize(itemId);
		ItemComposition itemComposition = itemManager.getItemComposition(canonicalId);
		LinkBrowser.browse(WIKI_LOOKUP.newBuilder()
			.addQueryParameter("type", "item")
			.addQueryParameter("id", String.valueOf(canonicalId))
			.addQueryParameter("name", Text.removeTags(itemComposition.getMembersName()))
			.build()
			.toString());
	}
}
