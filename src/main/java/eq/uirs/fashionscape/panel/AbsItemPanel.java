package eq.uirs.fashionscape.panel;

import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

abstract class AbsItemPanel extends AbsIconLabelPanel
{
	public final Integer itemId;

	protected final ItemManager itemManager;

	AbsItemPanel(@Nullable Integer itemId, BufferedImage icon, ItemManager itemManager,
				 ClientThread clientThread)
	{
		super(icon, clientThread);
		this.itemId = itemId;
		this.itemManager = itemManager;
		setItemName(itemId);
	}

	protected void setItemName(Integer itemId)
	{
		clientThread.invokeLater(() -> {
			String itemName = "Nothing";
			if (itemId != null)
			{
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				itemName = itemComposition.getName();
			}
			label.setText(itemName);
		});
	}
}
