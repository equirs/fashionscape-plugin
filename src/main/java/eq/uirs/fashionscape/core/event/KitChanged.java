package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.layer.ModelType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Value
public class KitChanged extends SwapEvent
{
	KitType slot;
	// new event data, will be null until migrated
	ModelType modelType;
	// will be null if kit removed
	Integer kitId;

	// legacy, will eventually remove
	public KitChanged(KitType slot, Integer kitId)
	{
		this(slot, null, kitId);
	}
}
