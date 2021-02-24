package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class PetLockChangedListener extends SwapEventListener<PetLockChanged>
{
	public PetLockChangedListener(Consumer<PetLockChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<PetLockChanged> getEventClass()
	{
		return PetLockChanged.class;
	}
}
