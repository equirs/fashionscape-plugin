package eq.uirs.fashionscape.core.utils;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class MapUtilTest
{
	@Test
	void noOverlapMerge()
	{
		Map<String, Integer> map = new HashMap<>(ImmutableMap.of("a", 1));
		MapUtil.putAllIfAllAbsent(map, ImmutableMap.of("b", 2, "c", 3));
		assertEquals(ImmutableMap.of("a", 1, "b", 2, "c", 3), map);
	}

	@Test
	void noOpWhenAnyKeysMatch()
	{
		Map<String, Integer> map = new HashMap<>(ImmutableMap.of("a", 1));
		MapUtil.putAllIfAllAbsent(map, ImmutableMap.of("a", 9, "b", 2));
		assertEquals(ImmutableMap.of("a", 1), map);
	}
}
