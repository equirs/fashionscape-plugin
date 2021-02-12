package eq.uirs.fashionscape.colors;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

@Value
public class ItemColors
{
	public List<ItemColorInfo> itemColorInfo;

	public static class Deserializer implements JsonDeserializer<ItemColors>
	{
		@Override
		public ItemColors deserialize(JsonElement jsonElement, Type type,
									  JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
		{
			List<ItemColorInfo> info = new ArrayList<>();
			JsonArray outer = jsonElement.getAsJsonArray();
			JsonArray rgbs = outer.get(0).getAsJsonArray();
			JsonArray pcts = outer.get(1).getAsJsonArray();
			for (int i = 0; i < rgbs.size(); i++)
			{
				int rgb = rgbs.get(i).getAsInt();
				double pct = pcts.get(i).getAsDouble();
				info.add(new ItemColorInfo(rgb, pct));
			}
			return new ItemColors(info);
		}
	}
}
