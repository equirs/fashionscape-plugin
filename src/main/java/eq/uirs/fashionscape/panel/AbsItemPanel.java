package eq.uirs.fashionscape.panel;

import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

@Slf4j
abstract class AbsItemPanel extends AbsIconLabelPanel
{
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
}
