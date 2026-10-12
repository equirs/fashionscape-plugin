package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.core.filter.Membership;
import eq.uirs.fashionscape.panel.NothingItemComposition;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
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
	private final ItemManager itemManager = mock(ItemManager.class);
	private final ItemSearch itemSearch = createItemSearch();

	private ItemSearch createItemSearch()
	{
		// no items, so only the "Nothing" result can match
		Exclusions exclusions = mock(Exclusions.class);
		when(exclusions.getAll()).thenReturn(Collections.emptySet());
		return new ItemSearch(mock(Client.class), itemManager, exclusions, mock(ColorScorer.class));
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

	@Test
	public void priceSortPutsMostValuableFirst()
	{
		SearchResult cheap = itemResult(1, 100);
		SearchResult pricey = itemResult(2, 5_000);
		SearchResult nothing = new SearchResult(null, KitType.HEAD, null, null);
		List<SearchResult> sorted = itemSearch.sort(Arrays.asList(cheap, nothing, pricey), SortBy.GE_PRICE, false);
		assertEquals(Arrays.asList(2, 1, NothingItemComposition.ID), ids(sorted));
		assertEquals(5_000L, sorted.get(0).getPrice());
		assertEquals(0L, sorted.get(2).getPrice());
		List<SearchResult> inverted = itemSearch.sort(Arrays.asList(cheap, nothing, pricey), SortBy.GE_PRICE, true);
		assertEquals(Arrays.asList(NothingItemComposition.ID, 1, 2), ids(inverted));
	}

	@Test
	public void otherSortsLeavePricesUnset()
	{
		List<SearchResult> sorted = itemSearch.sort(Collections.singletonList(itemResult(1, 100)),
			SortBy.ITEM_ID, false);
		assertNull(sorted.get(0).getPrice());
	}

	private SearchResult itemResult(int id, long price)
	{
		ItemComposition item = mock(ItemComposition.class);
		when(item.getId()).thenReturn(id);
		when(itemManager.getItemPrice(id)).thenReturn(price);
		return new SearchResult(item, KitType.HEAD, null, null);
	}

	private static List<Integer> ids(List<SearchResult> results)
	{
		return results.stream().map(SearchResult::getId).collect(Collectors.toList());
	}
}
