package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.core.event.FashionscapeEvent;
import eq.uirs.fashionscape.core.event.FashionscapeEventListener;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple eventbus for fashionscape-specific events
 */
@Slf4j
public class Events
{
	private static final Map<String, Queue<FashionscapeEventListener<? extends FashionscapeEvent>>> listeners = new ConcurrentHashMap<>();

	/**
	 * Adds a listener for a type of FashionscapeEvent.
	 * For panels that may be removed, call `removeListener` to clean up.
	 */
	public static void addListener(FashionscapeEventListener<? extends FashionscapeEvent> listener)
	{
		String key = listener.getKey();
		Queue<FashionscapeEventListener<?>> queue = listeners.getOrDefault(key, new ConcurrentLinkedQueue<>());
		queue.add(listener);
		listeners.put(key, queue);
	}

	public static void removeListener(FashionscapeEventListener<? extends FashionscapeEvent> listener)
	{
		Queue<FashionscapeEventListener<?>> queue = listeners.get(listener.getKey());
		if (queue == null)
		{
			return;
		}
		queue.remove(listener);
	}

	public static void removeListeners()
	{
		listeners.clear();
	}

	public static void fire(FashionscapeEvent event)
	{
		String key = event.getKey();
		listeners.getOrDefault(key, new ConcurrentLinkedQueue<>())
			.forEach(listener -> listener.onEvent(event));
	}
}
