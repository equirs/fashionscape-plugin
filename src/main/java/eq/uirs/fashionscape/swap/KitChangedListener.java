package eq.uirs.fashionscape.swap;

import java.util.function.Consumer;

public class KitChangedListener extends SwapEventListener<KitChanged>
{
	public KitChangedListener(Consumer<KitChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<KitChanged> getEventClass()
	{
		return KitChanged.class;
	}
}
