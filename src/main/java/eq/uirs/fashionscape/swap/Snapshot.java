package eq.uirs.fashionscape.swap;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@ToString
class Snapshot
{
	@Getter
	private final Map<KitType, Integer> changedEquipmentIds;
	@Getter
	private final Integer changedIdleAnimationId;

	boolean isEmpty()
	{
		return changedIdleAnimationId == null && changedEquipmentIds.isEmpty();
	}

	// This snapshot will take priority of the other snapshot in the event of a collision.
	Snapshot mergeWith(Snapshot other)
	{
		Map<KitType, Integer> merged = new HashMap<>();
		merged.putAll(other.changedEquipmentIds);
		merged.putAll(this.changedEquipmentIds);
		Integer idleId = this.changedIdleAnimationId;
		if (idleId == null)
		{
			idleId = other.changedIdleAnimationId;
		}
		return new Snapshot(merged, idleId);
	}
}
