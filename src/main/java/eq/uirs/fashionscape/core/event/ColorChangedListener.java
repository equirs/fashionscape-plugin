package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class ColorChangedListener extends FashionscapeEventListener<ColorChanged>
{
	public ColorChangedListener(Consumer<ColorChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<ColorChanged> getEventClass()
	{
		return ColorChanged.class;
	}
}
