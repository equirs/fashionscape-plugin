package eq.uirs.fashionscape.swap;

import eq.uirs.fashionscape.data.ColorType;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import net.runelite.api.kit.KitType;

/**
 * Holds onto information about equipment and animations before a swap occurred
 */
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
class SwapDiff
{
	@Value
	public static class Change
	{
		/**
		 * The changed equipment id or color id (depending on context)
		 */
		int id;
		/**
		 * Indicates that the previous state was "unnatural", i.e., originated from the plugin, not the game
		 */
		boolean unnatural;
	}

	public static SwapDiff blank()
	{
		return new SwapDiff(new HashMap<>(), new HashMap<>(), null);
	}

	@Getter
	private final Map<KitType, Change> slotChanges;
	@Getter
	private final Map<ColorType, Change> colorChanges;
	@Getter
	private final Integer changedIdleAnimationId;

	boolean isBlank()
	{
		return changedIdleAnimationId == null && slotChanges.isEmpty() && colorChanges.isEmpty();
	}

	// This diff will take priority of the other diff in the event of a collision.
	SwapDiff mergeOver(SwapDiff other)
	{
		Map<KitType, Change> mergedSlots = new HashMap<>();
		mergedSlots.putAll(other.slotChanges);
		mergedSlots.putAll(this.slotChanges);
		Map<ColorType, Change> mergedColors = new HashMap<>();
		mergedColors.putAll(other.colorChanges);
		mergedColors.putAll(this.colorChanges);
		Integer idleId = this.changedIdleAnimationId;
		if (idleId == null)
		{
			idleId = other.changedIdleAnimationId;
		}
		return new SwapDiff(mergedSlots, mergedColors, idleId);
	}
}
