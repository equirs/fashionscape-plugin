package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class KitChanged extends FashionscapeEvent
{
	KitType slot;
	ModelType modelType;
	// will be null if kit removed
	Integer kitId;
}
