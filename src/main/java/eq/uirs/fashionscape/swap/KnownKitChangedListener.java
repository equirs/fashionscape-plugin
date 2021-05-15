package eq.uirs.fashionscape.swap;

import java.util.function.Consumer;

public class KnownKitChangedListener extends SwapEventListener<KnownKitChanged>
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
