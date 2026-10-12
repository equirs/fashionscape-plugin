package eq.uirs.fashionscape.core.filter;

import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Item details that filters check
 */
@Value
public class ItemProperties
{
	// the "Nothing" result: f2p, untradeable, no stats or value
	public static final ItemProperties NOTHING = new ItemProperties(false, false, false, 0, 0);

	boolean members;
	boolean tradeable;
	boolean combatStats;
	long alchValue;
	long geValue;

	// call on the client thread
	public static ItemProperties of(ItemComposition item, ItemManager itemManager)
	{
		ItemStats stats = itemManager.getItemStats(item.getId());
		return new ItemProperties(item.isMembers(), item.isGeTradeable(),
			hasCombatStats(stats != null ? stats.getEquipment() : null),
			item.getHaPrice(), itemManager.getItemPrice(item.getId()));
	}

	private static boolean hasCombatStats(@Nullable ItemEquipmentStats e)
	{
		return e != null && (e.getAstab() != 0 || e.getAslash() != 0 || e.getAcrush() != 0 ||
			e.getAmagic() != 0 || e.getArange() != 0 || e.getDstab() != 0 || e.getDslash() != 0 ||
			e.getDcrush() != 0 || e.getDmagic() != 0 || e.getDrange() != 0 || e.getStr() != 0 ||
			e.getRstr() != 0 || e.getMdmg() != 0 || e.getPrayer() != 0);
	}
}
