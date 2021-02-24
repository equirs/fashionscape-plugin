package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class PetLockChanged extends SwapEvent
{
	boolean isLocked;
}
