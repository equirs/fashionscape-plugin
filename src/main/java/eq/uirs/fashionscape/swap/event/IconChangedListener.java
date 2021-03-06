package eq.uirs.fashionscape.swap.event;

import java.util.function.Consumer;

public class IconChangedListener extends SwapEventListener<IconChanged>
{
	public IconChangedListener(Consumer<IconChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<IconChanged> getEventClass()
	{
		return IconChanged.class;
	}
}
