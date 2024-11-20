package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class LockChangedListener extends FashionscapeEventListener<LockChanged>
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
