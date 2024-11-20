package eq.uirs.fashionscape.core.event;

import eq.uirs.fashionscape.core.LockStatus;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

@EqualsAndHashCode(callSuper = false)
@Value
public class LockChanged extends FashionscapeEvent
{
	KitType slot;
	@Nullable
	LockStatus status;
}
