package eq.uirs.fashionscape.panel.search;

import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ItemSearchTest
{
	@Test
	public void anySlotMatchesPanelSlotsOnly()
	{
		assertTrue(ItemSearch.matchesSlot(null, KitType.HEAD));
		assertFalse(ItemSearch.matchesSlot(null, KitType.HAIR));
	}

	@Test
	public void singleSlotMatchesOnlyThatSlot()
	{
		assertTrue(ItemSearch.matchesSlot(KitType.CAPE, KitType.CAPE));
		assertFalse(ItemSearch.matchesSlot(KitType.CAPE, KitType.HEAD));
	}
}
