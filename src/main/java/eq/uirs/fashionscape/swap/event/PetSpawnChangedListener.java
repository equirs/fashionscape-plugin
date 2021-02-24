package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class PetSpawnChangedListener extends SwapEventListener<PetSpawnChanged>
{
	public PetSpawnChangedListener(Consumer<PetSpawnChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<PetSpawnChanged> getEventClass()
	{
		return PetSpawnChanged.class;
	}
}
