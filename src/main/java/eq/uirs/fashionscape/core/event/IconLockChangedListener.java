package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class IconLockChangedListener extends FashionscapeEventListener<IconLockChanged>
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
