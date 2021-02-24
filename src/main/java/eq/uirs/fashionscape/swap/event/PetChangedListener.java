package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class PetChangedListener extends SwapEventListener<PetChanged>
{
	public PetChangedListener(Consumer<PetChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<PetChanged> getEventClass()
	{
		return PetChanged.class;
	}
}
