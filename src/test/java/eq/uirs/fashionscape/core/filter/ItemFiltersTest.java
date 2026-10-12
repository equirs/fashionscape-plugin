package eq.uirs.fashionscape.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ItemFiltersTest
{
	private static final ItemProperties F2P_TRADEABLE = new ItemProperties(false, true, false, 60, 100);
	private static final ItemProperties MEMBERS_WITH_STATS = new ItemProperties(true, false, true, 30_000, 0);

	@Test
	public void defaultMatchesEverything()
	{
		assertTrue(ItemFilters.DEFAULT.matches(F2P_TRADEABLE));
		assertTrue(ItemFilters.DEFAULT.matches(MEMBERS_WITH_STATS));
		assertTrue(ItemFilters.DEFAULT.matches(ItemProperties.NOTHING));
		assertEquals(0, ItemFilters.DEFAULT.activeCount());
	}

	@Test
	public void membershipMatchesMembersFlag()
	{
		ItemFilters f2p = ItemFilters.DEFAULT.withMembership(Membership.F2P);
		assertTrue(f2p.matches(F2P_TRADEABLE));
		assertFalse(f2p.matches(MEMBERS_WITH_STATS));
		assertTrue(ItemFilters.DEFAULT.withMembership(Membership.MEMBERS).matches(MEMBERS_WITH_STATS));
	}

	@Test
	public void triStatesMatchFlags()
	{
		assertFalse(ItemFilters.DEFAULT.withTradeable(TriState.YES).matches(MEMBERS_WITH_STATS));
		assertTrue(ItemFilters.DEFAULT.withTradeable(TriState.NO).matches(MEMBERS_WITH_STATS));
		assertTrue(ItemFilters.DEFAULT.withCombatStats(TriState.YES).matches(MEMBERS_WITH_STATS));
		assertFalse(ItemFilters.DEFAULT.withCombatStats(TriState.YES).matches(F2P_TRADEABLE));
	}

	@Test
	public void valueBoundsAreInclusiveAndUseSource()
	{
		ItemFilters geRange = ItemFilters.DEFAULT.withValueSource(ValueSource.GE).withMinValue(100).withMaxValue(200);
		assertTrue(geRange.matches(F2P_TRADEABLE));
		assertFalse(geRange.withMinValue(101).matches(F2P_TRADEABLE));
		assertFalse(geRange.withValueSource(ValueSource.ALCH).matches(F2P_TRADEABLE));
		assertTrue(ItemFilters.DEFAULT.withValueSource(ValueSource.ALCH).withMinValue(10_000)
			.matches(MEMBERS_WITH_STATS));
	}

	@Test
	public void valueRangeCountsAsOneFilter()
	{
		ItemFilters filters = ItemFilters.DEFAULT
			.withMembership(Membership.F2P)
			.withValueSource(ValueSource.GE)
			.withMinValue(1)
			.withMaxValue(5);
		assertEquals(2, filters.activeCount());
		assertEquals(0, ItemFilters.DEFAULT.withValueSource(ValueSource.ALCH).activeCount());
	}

	@Test
	public void anyValueSourceIgnoresRange()
	{
		ItemFilters filters = ItemFilters.DEFAULT.withMinValue(1_000_000);
		assertTrue(filters.matches(F2P_TRADEABLE));
		assertEquals(0, filters.activeCount());
	}
}
