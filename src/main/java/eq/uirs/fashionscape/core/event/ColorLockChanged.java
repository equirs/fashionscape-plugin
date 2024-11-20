package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.data.color.ColorType;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class ColorLockChanged extends FashionscapeEvent
{
	ColorType type;
	boolean isLocked;
}
