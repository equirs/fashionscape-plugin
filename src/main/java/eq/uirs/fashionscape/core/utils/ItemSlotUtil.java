package eq.uirs.fashionscape.core.utils;

import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.remote.RemoteData;
import javax.annotation.Nullable;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

public class ItemSlotUtil
{
	// only call from client thread
	@Nullable
	public static KitType getSlot(int itemId, ItemManager itemManager)
	{
		// see if external data can be used first
		KitType[] allKits = KitType.values();
		ItemSlotInfo slotInfo = RemoteData.ITEM_ID_TO_INFO.get(itemId);
		if (slotInfo != null && slotInfo.slot >= 0 && slotInfo.slot < allKits.length)
		{
			return allKits[slotInfo.slot];
		}
		ItemStats itemStats = itemManager.getItemStats(itemId);
		if (itemStats == null)
		{
			return null;
		}
		ItemEquipmentStats equipmentStats = itemStats.getEquipment();
		if (equipmentStats == null)
		{
			return null;
		}
		int slotIndex = equipmentStats.getSlot();
		if (slotIndex < 0 || slotIndex >= allKits.length)
		{
			return null;
		}
		return allKits[slotIndex];
	}
}
