package eq.uirs.fashionscape.swap;

import eq.uirs.fashionscape.data.ColorType;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class ColorChanged extends SwapEvent
{
	ColorType type;
	// will be null if color reverted
	Integer colorId;
}
