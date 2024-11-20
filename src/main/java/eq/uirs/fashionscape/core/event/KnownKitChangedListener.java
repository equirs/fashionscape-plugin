package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class KnownKitChangedListener extends FashionscapeEventListener<KnownKitChanged>
{
	public KnownKitChangedListener(Consumer<KnownKitChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<KnownKitChanged> getEventClass()
	{
		return KnownKitChanged.class;
	}
}
