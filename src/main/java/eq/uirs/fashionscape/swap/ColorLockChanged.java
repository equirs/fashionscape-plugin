package eq.uirs.fashionscape.swap;

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
