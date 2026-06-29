package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.ModelType;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class ItemChanged
{
	KitType slot;
	ModelType modelType;
	@Nullable
	SlotInfo newInfo;
}
