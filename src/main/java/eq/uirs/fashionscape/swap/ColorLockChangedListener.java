package eq.uirs.fashionscape.swap;

import java.util.function.Consumer;

public class ColorLockChangedListener extends SwapEventListener<ColorLockChanged>
{
	public ColorLockChangedListener(Consumer<ColorLockChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<ColorLockChanged> getEventClass()
	{
		return ColorLockChanged.class;
	}
}
