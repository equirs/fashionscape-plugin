package eq.uirs.fashionscape.swap;

import java.util.function.Consumer;

public class ItemChangedListener extends SwapEventListener<ItemChanged>
{
	public ItemChangedListener(Consumer<ItemChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<ItemChanged> getEventClass()
	{
		return ItemChanged.class;
	}
}
