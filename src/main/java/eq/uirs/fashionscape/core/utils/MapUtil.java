package eq.uirs.fashionscape.core.utils;

import java.util.Map;

public class MapUtil
{
	/**
	 * Adds contents of `other` to this `map` as long as all keys in `other` are not present in `map`.
	 */
	public static <K, V> void putAllIfAllAbsent(Map<K, V> map, Map<K, V> other)
	{
		for (K key : map.keySet())
		{
			if (other.containsKey(key))
			{
				return;
			}
		}
		map.putAll(other);
	}
}
