package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class SwapEventListener<T extends SwapEvent>
{
	private final Consumer<T> consumer;

	abstract Class<T> getEventClass();

	public String getKey()
	{
		return getEventClass().getName();
	}

	// onEvent is only ever called for events of type T
	@SuppressWarnings("unchecked")
	public void onEvent(SwapEvent event)
	{
		consumer.accept((T) event);
	}
}

