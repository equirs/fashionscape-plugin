package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.core.filter.Membership;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemSearchTest
{
	private final ItemSearch itemSearch = createItemSearch();

	private static ItemSearch createItemSearch()
	{
		// no items, so only the "Nothing" result can match
		Exclusions exclusions = mock(Exclusions.class);
		when(exclusions.getAll()).thenReturn(Collections.emptySet());
		return new ItemSearch(mock(Client.class), mock(ItemManager.class), exclusions, mock(ColorScorer.class));
	}

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

	@Test
	public void nothingResultUsesTheSearchedSlot()
	{
		List<SearchResult> head = itemSearch.find("noth", KitType.HEAD, ItemFilters.DEFAULT);
		List<SearchResult> cape = itemSearch.find("noth", KitType.CAPE, ItemFilters.DEFAULT);
		assertEquals(1, head.size());
		assertNull(head.get(0).getItemComposition());
		assertEquals(KitType.HEAD, head.get(0).getSlot());
		assertEquals(KitType.CAPE, cape.get(0).getSlot());
	}

	@Test
	public void nothingResultNeverShowsForAnySlot()
	{
		assertTrue(itemSearch.find("nothing", null, ItemFilters.DEFAULT).isEmpty());
	}

	@Test
	public void nothingResultObeysFilters()
	{
		ItemFilters members = ItemFilters.DEFAULT.withMembership(Membership.MEMBERS);
		assertTrue(itemSearch.find("", KitType.HEAD, members).isEmpty());
		assertTrue(itemSearch.find("", KitType.HANDS, ItemFilters.DEFAULT).isEmpty());
	}
}
