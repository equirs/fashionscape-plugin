package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.LockStatus;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Value
public class LockChanged extends SwapEvent
{
	KitType slot;
	boolean isLocked;
	// (legacy only) null for new-shape construction
	@Nullable
	Type type;
	// new event data, will be null until migrated
	@Nullable
	LockStatus status;

	// legacy, will eventually remove
	public LockChanged(KitType slot, boolean isLocked, Type type)
	{
		this(slot, isLocked, type, null);
	}

	// v2 constructor
	public LockChanged(KitType slot, LockStatus status)
	{
		this(slot, false, null, status);
	}

	public enum Type
	{
		ITEM,
		KIT,
		BOTH
	}
}
