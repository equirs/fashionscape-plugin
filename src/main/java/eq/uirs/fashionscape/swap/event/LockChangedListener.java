package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class LockChangedListener extends SwapEventListener<LockChanged>
{
	public LockChangedListener(Consumer<LockChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<LockChanged> getEventClass()
	{
		return LockChanged.class;
	}
}
