package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class ItemChangedListener extends FashionscapeEventListener<ItemChanged>
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
