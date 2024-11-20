package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class KitChangedListener extends FashionscapeEventListener<KitChanged>
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
