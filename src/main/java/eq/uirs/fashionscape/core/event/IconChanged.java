package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Value
public class IconChanged extends SwapEvent
{
	JawIcon icon;
	// new event data, will be null until migrated
	ModelType modelType;

	// legacy, will eventually remove
	public IconChanged(JawIcon icon)
	{
		this(icon, null);
	}
}
