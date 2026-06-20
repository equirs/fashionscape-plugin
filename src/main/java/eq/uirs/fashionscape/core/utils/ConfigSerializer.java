package eq.uirs.fashionscape.core.utils;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigSerializer
{
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private final Gson gson;

	@Inject
	ConfigSerializer(Gson gson)
	{
		this.gson = gson;
	}

	public byte[] serialize(Object object)
	{
		return gson.toJson(object).getBytes(CHARSET);
	}

	@Nullable
	public <T> T deserialize(@Nullable byte[] bytes, Type type)
	{
		if (bytes == null)
		{
			return null;
		}
		return gson.fromJson(new String(bytes, CHARSET), type);
	}
}
