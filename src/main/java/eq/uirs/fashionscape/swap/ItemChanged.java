package eq.uirs.fashionscape.swap;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class ItemChanged extends SwapEvent
{
	KitType slot;
	// will be null if an item has been removed
	Integer itemId;
}
