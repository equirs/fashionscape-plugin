package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class LockChanged extends SwapEvent
{
	KitType slot;
	boolean isLocked;
	Type type;

	public enum Type
	{
		ITEM,
		KIT,
		BOTH
	}
}
