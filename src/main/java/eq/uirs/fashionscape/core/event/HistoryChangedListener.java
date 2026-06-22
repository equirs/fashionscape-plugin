package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;

public class HistoryChangedListener extends FashionscapeEventListener<HistoryChanged>
{
	public HistoryChangedListener(Consumer<HistoryChanged> consumer)
	{
		super(consumer);
	}

	@Override
	Class<HistoryChanged> getEventClass()
	{
		return HistoryChanged.class;
	}
}
