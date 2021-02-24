package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class PetSpawnChanged extends SwapEvent
{
	boolean spawned;
}
