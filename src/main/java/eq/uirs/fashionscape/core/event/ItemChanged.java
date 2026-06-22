package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.ModelType;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Value
public class ItemChanged extends SwapEvent
{
	KitType slot;
	// new event data, will be null until migrated
	ModelType modelType;
	// (legacy only) null if a virtual item has been removed, -1 when hidden (over a real item)
	Integer itemId;
	@Nullable
	SlotInfo newInfo;

	// legacy, will eventually remove
	public ItemChanged(KitType slot, Integer itemId)
	{
		this(slot, null, itemId, null);
	}

	// v2 constructor
	public ItemChanged(KitType slot, ModelType modelType, @Nullable SlotInfo newInfo)
	{
		this(slot, modelType, null, newInfo);
	}
}
