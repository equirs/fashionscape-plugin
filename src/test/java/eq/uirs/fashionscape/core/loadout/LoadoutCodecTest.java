package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.kit.KitType;
import org.junit.jupiter.api.Test;

public class LoadoutCodecTest
{
	private static final String DEFAULT_NAME = "Default";

	private final LoadoutCodec codec = new LoadoutCodec(new Gson());

	@Test
	void singleLoadoutRoundTrips()
	{
		Loadout loadout = sample("Void");
		String json = codec.toJson(loadout);
		assertTrue(json.contains("\"version\":1"));
		assertEquals(ImmutableList.of(loadout), codec.parse(json, DEFAULT_NAME));
	}

	@Test
	void listRoundTrips()
	{
		List<Loadout> loadouts = ImmutableList.of(sample("a"), sample("b"));
		assertEquals(loadouts, codec.parse(codec.toJson(loadouts), DEFAULT_NAME));
	}

	@Test
	void readsLegacyText()
	{
		List<Loadout> parsed = codec.parse("HEAD:1163 (Rune full helm)\nSKIN_COLOR:4", DEFAULT_NAME);
		assertEquals(1, parsed.size());
		assertEquals(DEFAULT_NAME, parsed.get(0).getName());
		assertEquals(ImmutableMap.of(KitType.HEAD, 1163), parsed.get(0).getItems());
	}

	@Test
	void dropsWhatItCantRead()
	{
		String json = "{\"name\":\"  \",\"items\":{\"HEAD\":-7,\"NOT_A_SLOT\":5},\"colors\":{\"SKIN\":null}}";
		Loadout parsed = codec.parse(json, DEFAULT_NAME).get(0);
		assertEquals(DEFAULT_NAME, parsed.getName());
		assertEquals(ImmutableMap.of(KitType.HEAD, Loadout.NOTHING), parsed.getItems());
		assertTrue(parsed.getColors().isEmpty());
	}

	@Test
	void rejectsNewerVersion()
	{
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> codec.parse("{\"version\":2,\"items\":{\"HEAD\":1163}}", DEFAULT_NAME));
		assertTrue(e.getMessage().contains("newer version"));
	}

	@Test
	void rejectsInvalidData()
	{
		assertThrows(IllegalArgumentException.class, () -> codec.parse("{not json", DEFAULT_NAME));
		assertThrows(IllegalArgumentException.class, () -> codec.parse("[1, 2]", DEFAULT_NAME));
		assertThrows(IllegalArgumentException.class, () -> codec.parse("{\"version\":\"x\"}", DEFAULT_NAME));
		assertThrows(IllegalArgumentException.class, () -> codec.parse("just some text", DEFAULT_NAME));
		assertThrows(IllegalArgumentException.class, () -> codec.parse("{}", DEFAULT_NAME));
	}

	private static Loadout sample(String name)
	{
		Loadout loadout = new Loadout().withName(name);
		loadout.getItems().put(KitType.HEAD, 1163);
		loadout.getItems().put(KitType.SHIELD, Loadout.NOTHING);
		loadout.getKits().put(KitType.HAIR, 3);
		loadout.getColors().put(ColorType.SKIN, 4);
		loadout.setIcon(JawIcon.BA_HEALER);
		return loadout;
	}
}
