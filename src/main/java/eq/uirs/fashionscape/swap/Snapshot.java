package eq.uirs.fashionscape.swap;

import eq.uirs.fashionscape.data.ColorType;
import java.util.HashMap;
import java.util.Map;
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
class Snapshot
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

	@Getter
	private final Map<KitType, Change> slotChanges;
	@Getter
	private final Map<ColorType, Change> colorChanges;
	@Getter
	private final Integer changedIdleAnimationId;

	boolean isEmpty()
	{
		return changedIdleAnimationId == null && slotChanges.isEmpty();
	}

	// This snapshot will take priority of the other snapshot in the event of a collision.
	Snapshot mergeOver(Snapshot other)
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
		return new Snapshot(mergedSlots, mergedColors, idleId);
	}
}
