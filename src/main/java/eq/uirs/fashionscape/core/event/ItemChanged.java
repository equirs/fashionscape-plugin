package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.ModelType;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = false)
@Value
public class ItemChanged extends FashionscapeEvent
{
	KitType slot;
	ModelType modelType;
	@Nullable
	SlotInfo newInfo;
}
