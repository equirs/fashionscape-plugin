package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = false)
@Value
public class PetChanged extends SwapEvent
{
	// will be null if pet removed
	Integer petId;
}
