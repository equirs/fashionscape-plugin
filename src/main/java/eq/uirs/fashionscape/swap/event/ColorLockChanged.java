package eq.uirs.fashionscape.swap.event;

import eq.uirs.fashionscape.data.ColorType;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class ColorLockChanged extends SwapEvent
{
	ColorType type;
	boolean isLocked;
}
