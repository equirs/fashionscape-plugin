package eq.uirs.fashionscape.core.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class KitChanged extends SwapEvent
{
	KitType slot;
	// will be null if kit removed
	Integer kitId;
}
