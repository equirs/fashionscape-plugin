package eq.uirs.fashionscape.swap.event;

import eq.uirs.fashionscape.data.kit.JawIcon;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class IconChanged extends SwapEvent
{
	JawIcon icon;
}
