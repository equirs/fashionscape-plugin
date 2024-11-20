package eq.uirs.fashionscape.core.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class IconLockChanged extends FashionscapeEvent
{
	boolean isLocked;
}

