package eq.uirs.fashionscape.panel.search;

import com.google.common.primitives.Ints;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.core.filter.ItemProperties;
import eq.uirs.fashionscape.core.utils.ItemSlotUtil;
import eq.uirs.fashionscape.panel.NothingItemComposition;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

/**
 * Finds and sorts items for the search panel
 */
@RequiredArgsConstructor(onConstructor_ = {@Inject})
class ItemSearch
{
	private static final Set<KitType> VALID_SLOTS = Arrays.stream(PanelEquipSlot.values())
		.map(PanelEquipSlot::getKitType)
		.filter(Objects::nonNull)
		.collect(Collectors.toSet());
	private static final Comparator<SearchResult> BY_ID = Comparator.comparingInt(SearchResult::getId);
	private static final Comparator<SearchResult> BY_NAME = Comparator.comparing(SearchResult::getName);
	// best match first
	private static final Comparator<SearchResult> BY_SCORE = Comparator.comparingDouble(r ->
		r.getScore() == null ? 0.0 : -r.getScore());
	// most valuable first
	private static final Comparator<SearchResult> BY_PRICE = Comparator.comparingLong(r ->
		r.getPrice() == null ? 0L : -r.getPrice());

	private final Client client;
	private final ItemManager itemManager;
	private final Exclusions exclusions;
	private final ColorScorer colorScorer;

	private int largestSlotSize = 0;

	/**
	 * Returns valid items for the case-insensitive query in the given slot (null for any) that pass the filters.
	 * Exact item id match skips the slot, filters and exclusions. Call on the client thread.
	 */
	List<SearchResult> find(String query, @Nullable KitType slot, ItemFilters filters)
	{
		List<SearchResult> results = new ArrayList<>();
		if (filters.matches(ItemProperties.NOTHING))
		{
			addNothingResult(results, query, slot);
		}
		Integer queryId = Ints.tryParse(query);
		Set<Integer> skips = exclusions.getAll();
		for (int i = 0; i < client.getItemCount(); i++)
		{
			ItemComposition item = canonicalComposition(i);
			if (item == null)
			{
				continue;
			}
			boolean idMatch = Objects.equals(queryId, i);
			boolean nameMatch = !skips.contains(i) && nameMatches(item, query);
			if (!idMatch && !nameMatch)
			{
				continue;
			}
			KitType itemSlot = ItemSlotUtil.getSlot(i, itemManager);
			if (itemSlot != null && (idMatch || (matchesSlot(slot, itemSlot) && matchesFilters(filters, item))))
			{
				results.add(new SearchResult(item, itemSlot, null, null));
			}
		}
		return results;
	}

	/**
	 * Returns the item count of the slot with the most items, with no query or filters. Call on the client thread.
	 */
	int largestSlotSize()
	{
		if (largestSlotSize > 0)
		{
			return largestSlotSize;
		}
		Map<KitType, Integer> counts = new EnumMap<>(KitType.class);
		Set<Integer> skips = exclusions.getAll();
		for (int i = 0; i < client.getItemCount(); i++)
		{
			ItemComposition item = canonicalComposition(i);
			if (item == null || skips.contains(i) || !nameMatches(item, ""))
			{
				continue;
			}
			KitType itemSlot = ItemSlotUtil.getSlot(i, itemManager);
			if (itemSlot != null && VALID_SLOTS.contains(itemSlot))
			{
				counts.merge(itemSlot, 1, Integer::sum);
			}
		}
		int size = counts.values().stream().max(Integer::compare).orElse(0);
		// counts change until exclusions and the remote slot data they depend on have loaded
		if (exclusions.isLoaded())
		{
			largestSlotSize = size;
		}
		return size;
	}

	/**
	 * Returns results in sort order. Call on the client thread, except for color match, which is slow and
	 * doesn't need it.
	 */
	List<SearchResult> sort(List<SearchResult> results, SortBy sort, boolean inverted)
	{
		List<SearchResult> sorted = withSortValues(results, sort);
		Comparator<SearchResult> comparator = comparatorFor(sort);
		sorted.sort(inverted ? comparator.reversed() : comparator);
		return sorted;
	}

	private static Comparator<SearchResult> comparatorFor(SortBy sort)
	{
		switch (sort)
		{
			case ALPHABETICAL:
				return BY_NAME;
			case COLOR_MATCH:
				return BY_SCORE;
			case GE_PRICE:
				return BY_PRICE;
			default:
				return BY_ID;
		}
	}

	// returns a mutable copy of the results, with the score or price that the sort needs
	private List<SearchResult> withSortValues(List<SearchResult> results, SortBy sort)
	{
		switch (sort)
		{
			case COLOR_MATCH:
				return scoreColors(results);
			case GE_PRICE:
				return results.stream()
					.map(r -> r.withPrice(r.getItemComposition() != null ? itemManager.getItemPrice(r.getId()) : 0))
					.collect(Collectors.toList());
			default:
				return new ArrayList<>(results);
		}
	}

	// leaves scores unset when every score is 0, so rows don't all show 0%
	private List<SearchResult> scoreColors(List<SearchResult> results)
	{
		colorScorer.updatePlayerInfo();
		List<SearchResult> scored = results.stream()
			.map(r -> r.withScore(colorScorer.score(r.getId(), r.getSlot())))
			.collect(Collectors.toList());
		boolean anyScored = scored.stream().anyMatch(r -> r.getScore() != null && r.getScore() != 0.0);
		return anyScored ? scored : new ArrayList<>(results);
	}

	static boolean matchesSlot(@Nullable KitType slot, KitType itemSlot)
	{
		// "any slot" still leaves out slots the panel can't show (e.g. ammo)
		return slot == null ? VALID_SLOTS.contains(itemSlot) : slot == itemSlot;
	}

	private boolean matchesFilters(ItemFilters filters, ItemComposition item)
	{
		// skips the stat and price lookups when nothing is filtered
		return filters.activeCount() == 0 || filters.matches(ItemProperties.of(item, itemManager));
	}

	private void addNothingResult(List<SearchResult> results, String query, @Nullable KitType slot)
	{
		if (slot != null && FashionManager.ALLOWS_NOTHING_ITEMS.contains(slot) &&
			NothingItemComposition.NAME.toLowerCase().contains(query))
		{
			results.add(new SearchResult(null, slot, null, null));
		}
	}

	// returns null for non-canonical ids (e.g. noted items) and ids the client can't load
	@Nullable
	private ItemComposition canonicalComposition(int itemId)
	{
		try
		{
			return itemManager.canonicalize(itemId) == itemId ? itemManager.getItemComposition(itemId) : null;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private static boolean nameMatches(ItemComposition item, String query)
	{
		String name = item.getMembersName().toLowerCase();
		return !name.equals("null") && name.contains(query);
	}
}
