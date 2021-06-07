package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class IconLockChangedListener extends SwapEventListener<IconLockChanged>
{
	public IconLockChangedListener(Consumer<IconLockChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<IconLockChanged> getEventClass()
	{
		return IconLockChanged.class;
	}
}
