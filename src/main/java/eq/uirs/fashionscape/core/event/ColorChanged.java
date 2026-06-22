package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.color.ColorType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Value
public class ColorChanged extends SwapEvent
{
	ColorType type;
	// new event data, will be null until migrated
	ModelType modelType;
	// will be null if color reverted
	Integer colorId;

	// legacy, will eventually remove
	public ColorChanged(ColorType type, Integer colorId)
	{
		this(type, null, colorId);
	}
}
