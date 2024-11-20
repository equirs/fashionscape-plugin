package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class IconChanged extends FashionscapeEvent
{
	JawIcon icon;
	ModelType modelType;
}
