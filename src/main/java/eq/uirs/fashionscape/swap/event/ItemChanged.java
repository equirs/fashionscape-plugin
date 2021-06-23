package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class ItemChanged extends SwapEvent
{
	KitType slot;
	// will be null if a virtual item has been removed, and -1 when item is hidden (over a real item)
	Integer itemId;
}
