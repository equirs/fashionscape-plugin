package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class IconChangedListener extends FashionscapeEventListener<IconChanged>
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
