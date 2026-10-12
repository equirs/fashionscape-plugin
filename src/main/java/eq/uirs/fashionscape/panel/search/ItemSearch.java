package eq.uirs.fashionscape.panel.search;

import com.google.common.primitives.Ints;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.utils.ItemSlotUtil;
import eq.uirs.fashionscape.panel.NothingItemComposition;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

/**
 * Finds and sorts items for the search panel
 */
@Slf4j
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

	private final Client client;
	private final ItemManager itemManager;
	private final Exclusions exclusions;
	private final ColorScorer colorScorer;

	/**
	 * Returns valid items for the case-insensitive query in the given slot (null for any).
	 * Exact item id match skips filters and exclusions. Call on the client thread.
	 */
	List<SearchResult> find(String query, @Nullable KitType slot)
	{
		List<SearchResult> results = new ArrayList<>();
		addNothingResult(results, query, slot);
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
			if (itemSlot != null && (idMatch || matchesSlot(slot, itemSlot)))
			{
				addResult(results, item, itemSlot);
			}
		}
		return results;
	}

	/**
	 * Returns results in sort order. Color match is slow; call off the client thread.
	 */
	List<SearchResult> sort(List<SearchResult> results, SortBy sort, boolean inverted)
	{
		List<SearchResult> sorted = sort == SortBy.COLOR_MATCH ? scoreColors(results) : new ArrayList<>(results);
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
			default:
				return BY_ID;
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

	private void addNothingResult(List<SearchResult> results, String query, @Nullable KitType slot)
	{
		if (slot != null && FashionManager.ALLOWS_NOTHING_ITEMS.contains(slot) &&
			NothingItemComposition.NAME.toLowerCase().contains(query))
		{
			results.add(new SearchResult(null, PanelUtil.image(slot.name().toLowerCase()), slot, null));
		}
	}

	private void addResult(List<SearchResult> results, ItemComposition item, KitType slot)
	{
		try
		{
			results.add(new SearchResult(item, itemManager.getImage(item.getId()), slot, null));
		}
		catch (Exception e)
		{
			log.debug("could not get image for item id {}", item.getId());
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
