package eq.uirs.fashionscape.swap;

import java.util.function.Consumer;

public class ColorChangedListener extends SwapEventListener<ColorChanged>
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
