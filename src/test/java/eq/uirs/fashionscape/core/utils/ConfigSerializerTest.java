package eq.uirs.fashionscape.core.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class ConfigSerializerTest
{
	private final ConfigSerializer serializer = new ConfigSerializer(new Gson());

	@Test
	void thereAndBack()
	{
		Map<String, Integer> original = ImmutableMap.of("a", 1, "b", 2);
		Type type = new TypeToken<Map<String, Integer>>()
		{
		}.getType();
		byte[] bytes = serializer.serialize(original);
		assertEquals(original, serializer.deserialize(bytes, type));
	}

	@Test
	void nullBytes()
	{
		assertNull(serializer.deserialize(null, String.class));
	}
}
