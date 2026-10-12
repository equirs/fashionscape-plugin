package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.core.filter.Membership;
import eq.uirs.fashionscape.core.filter.TriState;
import eq.uirs.fashionscape.core.filter.ValueSource;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Persists item filters and which filter panel sections are open in config, one key per setting
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
class FilterStore
{
	private static final String KEY_MEMBERSHIP = "searchMembership";
	private static final String KEY_TRADEABLE = "searchTradeable";
	private static final String KEY_COMBAT_STATS = "searchCombatStats";
	private static final String KEY_VALUE_SOURCE = "searchValueSource";
	private static final String KEY_MIN_VALUE = "searchMinValue";
	private static final String KEY_MAX_VALUE = "searchMaxValue";
	private static final String KEY_SLOTS_EXPANDED = "searchSlotsExpanded";
	private static final String KEY_FILTERS_EXPANDED = "searchFiltersExpanded";

	private final ConfigManager configManager;
	private final FashionscapeConfig config;

	ItemFilters load()
	{
		ItemFilters defaults = ItemFilters.DEFAULT;
		Membership membership = get(KEY_MEMBERSHIP, Membership.class);
		boolean firstLoad = membership == null;
		if (firstLoad)
		{
			// carries over the old "exclude members items" toggle
			membership = config.excludeMembersItems() ? Membership.F2P : defaults.getMembership();
		}
		ItemFilters filters = defaults
			.withMembership(membership)
			.withTradeable(getOrDefault(KEY_TRADEABLE, TriState.class, defaults.getTradeable()))
			.withCombatStats(getOrDefault(KEY_COMBAT_STATS, TriState.class, defaults.getCombatStats()))
			.withValueSource(getOrDefault(KEY_VALUE_SOURCE, ValueSource.class, defaults.getValueSource()))
			.withMinValue(get(KEY_MIN_VALUE, Integer.class))
			.withMaxValue(get(KEY_MAX_VALUE, Integer.class));
		if (firstLoad)
		{
			// later changes to the old toggle no longer affect search
			save(filters);
		}
		return filters;
	}

	void save(ItemFilters filters)
	{
		set(KEY_MEMBERSHIP, filters.getMembership());
		set(KEY_TRADEABLE, filters.getTradeable());
		set(KEY_COMBAT_STATS, filters.getCombatStats());
		set(KEY_VALUE_SOURCE, filters.getValueSource());
		set(KEY_MIN_VALUE, filters.getMinValue());
		set(KEY_MAX_VALUE, filters.getMaxValue());
	}

	// the slots section starts open. slots used to be always visible
	boolean isSlotsExpanded()
	{
		return getOrDefault(KEY_SLOTS_EXPANDED, Boolean.class, true);
	}

	void setSlotsExpanded(boolean expanded)
	{
		set(KEY_SLOTS_EXPANDED, expanded);
	}

	boolean isFiltersExpanded()
	{
		return getOrDefault(KEY_FILTERS_EXPANDED, Boolean.class, false);
	}

	void setFiltersExpanded(boolean expanded)
	{
		set(KEY_FILTERS_EXPANDED, expanded);
	}

	private <T> T getOrDefault(String key, Class<T> type, T fallback)
	{
		T value = get(key, type);
		return value != null ? value : fallback;
	}

	@Nullable
	private <T> T get(String key, Class<T> type)
	{
		try
		{
			return configManager.getConfiguration(FashionscapeConfig.GROUP, key, type);
		}
		catch (IllegalArgumentException e)
		{
			log.debug("ignoring invalid search filter config {}", key, e);
			return null;
		}
	}

	private void set(String key, @Nullable Object value)
	{
		if (value == null)
		{
			configManager.unsetConfiguration(FashionscapeConfig.GROUP, key);
		}
		else
		{
			configManager.setConfiguration(FashionscapeConfig.GROUP, key, value);
		}
	}
}
