package eq.uirs.fashionscape.core.filter;

import javax.annotation.Nullable;
import lombok.Value;
import lombok.With;

/**
 * Criteria that narrow down a set of items. Each criterion matches everything by default.
 */
@Value
@With
public class ItemFilters
{
	public static final ItemFilters DEFAULT = new ItemFilters(Membership.ANY, TriState.ANY, TriState.ANY,
		ValueSource.ANY, null, null);

	Membership membership;
	TriState tradeable;
	TriState combatStats;
	ValueSource valueSource;
	// inclusive bounds, ignored while valueSource is any
	@Nullable
	Integer minValue;
	@Nullable
	Integer maxValue;

	public boolean matches(ItemProperties item)
	{
		return membership.matches(item.isMembers()) &&
			tradeable.matches(item.isTradeable()) &&
			combatStats.matches(item.isCombatStats()) &&
			matchesValue(item);
	}

	public int activeCount()
	{
		int count = 0;
		count += membership != Membership.ANY ? 1 : 0;
		count += tradeable != TriState.ANY ? 1 : 0;
		count += combatStats != TriState.ANY ? 1 : 0;
		count += hasValueRange() ? 1 : 0;
		return count;
	}

	private boolean hasValueRange()
	{
		return valueSource != ValueSource.ANY && (minValue != null || maxValue != null);
	}

	private boolean matchesValue(ItemProperties item)
	{
		if (!hasValueRange())
		{
			return true;
		}
		long value = valueSource == ValueSource.ALCH ? item.getAlchValue() : item.getGeValue();
		return (minValue == null || value >= minValue) && (maxValue == null || value <= maxValue);
	}
}
