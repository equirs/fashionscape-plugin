package eq.uirs.fashionscape.panel;

import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

@Slf4j
abstract class AbsItemPanel extends AbsIconLabelPanel
{
	protected final ItemManager itemManager;
	private final boolean developerMode;

	AbsItemPanel(BufferedImage icon, ItemManager itemManager,
				 ClientThread clientThread, boolean developerMode)
	{
		super(icon, clientThread);
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
			if (itemId != null && developerMode)
			{
				label.setToolTipText("item id " + itemId);
			}
			return true;
		});
	}
}
