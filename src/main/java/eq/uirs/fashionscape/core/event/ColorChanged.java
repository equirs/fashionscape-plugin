package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.color.ColorType;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class ColorChanged extends FashionscapeEvent
{
	ColorType type;
	ModelType modelType;
	// will be null if color reverted
	Integer colorId;
}
