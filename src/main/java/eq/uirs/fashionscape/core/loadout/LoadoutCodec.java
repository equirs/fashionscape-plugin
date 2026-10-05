package eq.uirs.fashionscape.core.loadout;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.kit.KitType;

/**
 * Converts to and from the JSON sharing format, also supports the legacy txt format
 */
@Singleton
public class LoadoutCodec
{
	private static final int VERSION = 1;
	private static final String VERSION_KEY = "version";

	private final Gson gson;
	private final Gson prettyGson;

	@Inject
	LoadoutCodec(Gson gson)
	{
		this.gson = gson;
		this.prettyGson = gson.newBuilder().setPrettyPrinting().create();
	}

	public String toJson(Loadout loadout)
	{
		return gson.toJson(toTree(loadout));
	}

	// pretty printed, since files are more likely to be read by people
	public String toJson(List<Loadout> loadouts)
	{
		JsonArray array = new JsonArray();
		loadouts.forEach(l -> array.add(toTree(l)));
		return prettyGson.toJson(array);
	}

	/**
	 * Returns the loadouts in the text, which can be one JSON loadout, a JSON array, or legacy text.
	 * Loadouts without a name get the default name.
	 *
	 * @throws IllegalArgumentException if the text has no loadouts
	 */
	public List<Loadout> parse(String text, String defaultName)
	{
		String trimmed = text.trim();
		List<Loadout> parsed = new ArrayList<>();
		if (trimmed.startsWith("{") || trimmed.startsWith("["))
		{
			try
			{
				JsonElement json = gson.fromJson(trimmed, JsonElement.class);
				Iterable<JsonElement> elements = json.isJsonArray() ? json.getAsJsonArray() : Collections.singletonList(json);
				for (JsonElement element : elements)
				{
					parsed.add(fromTree(element.getAsJsonObject()));
				}
			}
			catch (JsonParseException | IllegalStateException | UnsupportedOperationException | NumberFormatException e)
			{
				throw new IllegalArgumentException("The loadout data is not valid.", e);
			}
		}
		else
		{
			parsed.add(LegacyFormat.parse(Arrays.asList(trimmed.split("\\R"))).getLoadout());
		}

		List<Loadout> result = new ArrayList<>();
		for (Loadout loadout : parsed)
		{
			Loadout clean = sanitize(loadout, defaultName);
			if (!clean.isEmpty())
			{
				result.add(clean);
			}
		}
		if (result.isEmpty())
		{
			throw new IllegalArgumentException("No loadouts were found.");
		}
		return result;
	}

	/**
	 * Returns a default loadout name for a file (e.g. "void.txt" becomes "void").
	 */
	public static String nameFromFile(String fileName)
	{
		int dot = fileName.lastIndexOf('.');
		return dot > 0 ? fileName.substring(0, dot) : fileName;
	}

	private JsonObject toTree(Loadout loadout)
	{
		JsonObject tree = gson.toJsonTree(loadout).getAsJsonObject();
		tree.addProperty(VERSION_KEY, VERSION);
		return tree;
	}

	private Loadout fromTree(JsonObject tree)
	{
		JsonElement version = tree.get(VERSION_KEY);
		if (version != null && version.getAsInt() > VERSION)
		{
			throw new IllegalArgumentException("This loadout is from a newer version of Fashionscape. Update the plugin to import it.");
		}
		return gson.fromJson(tree, Loadout.class);
	}

	// imports may be hand-edited, so drop entries the plugin can't read, like unknown slot names
	private static Loadout sanitize(Loadout loadout, String defaultName)
	{
		String name = loadout.getName() != null ? loadout.getName().trim() : "";
		Map<KitType, Integer> items = clean(loadout.getItems());
		items.replaceAll((slot, id) -> id < 0 ? Loadout.NOTHING : id);
		return new Loadout(name.isEmpty() ? defaultName : name, items, clean(loadout.getKits()),
			clean(loadout.getColors()), loadout.getIcon());
	}

	private static <K> Map<K, Integer> clean(@Nullable Map<K, Integer> map)
	{
		Map<K, Integer> result = new HashMap<>();
		if (map != null)
		{
			map.forEach((key, value) -> {
				if (key != null && value != null)
				{
					result.put(key, value);
				}
			});
		}
		return result;
	}
}
