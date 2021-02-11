package eq.uirs.fashionscape.swap;

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
		int equipmentId;
		/**
		 * Indicates that the previous item was equipped via the plugin
		 */
		boolean unnatural;
	}

	@Getter
	private final Map<KitType, Change> slotChanges;
	@Getter
	private final Integer changedIdleAnimationId;

	boolean isEmpty()
	{
		return changedIdleAnimationId == null && slotChanges.isEmpty();
	}

	// This snapshot will take priority of the other snapshot in the event of a collision.
	Snapshot mergeOver(Snapshot other)
	{
		Map<KitType, Change> merged = new HashMap<>();
		merged.putAll(other.slotChanges);
		merged.putAll(this.slotChanges);
		Integer idleId = this.changedIdleAnimationId;
		if (idleId == null)
		{
			idleId = other.changedIdleAnimationId;
		}
		return new Snapshot(merged, idleId);
	}
}
