package eq.uirs.fashionscape.core.event;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FashionscapeEventListener<T extends FashionscapeEvent>
{
	private final Consumer<T> consumer;

	abstract Class<T> getEventClass();

	public String getKey()
	{
		return getEventClass().getName();
	}

	// onEvent is only ever called for events of type T
	@SuppressWarnings("unchecked")
	public void onEvent(FashionscapeEvent event)
	{
		consumer.accept((T) event);
	}
}

