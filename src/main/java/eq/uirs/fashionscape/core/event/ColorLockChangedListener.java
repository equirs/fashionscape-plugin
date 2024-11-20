package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class ColorLockChangedListener extends FashionscapeEventListener<ColorLockChanged>
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
