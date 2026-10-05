package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.kit.KitType;
import org.junit.jupiter.api.Test;

public class LegacyFormatTest
{
	@Test
	void parsesEachEntryType()
	{
		List<String> lines = ImmutableList.of(
			"HEAD:1163 (Rune full helm)",
			"SHIELD:-1 (Nothing)",
			"HAIR_KIT:3 (Bald)",
			"SKIN_COLOR:4 (Dark)",
			"ICON:10556 (Attacker icon)",
			"",
			"not an entry"
		);
		LegacyFormat.ParseResult result = LegacyFormat.parse(lines);
		Loadout loadout = result.getLoadout();
		assertEquals(ImmutableMap.of(KitType.HEAD, 1163, KitType.SHIELD, Loadout.NOTHING), loadout.getItems());
		assertEquals(ImmutableMap.of(KitType.HAIR, 3), loadout.getKits());
		assertEquals(ImmutableMap.of(ColorType.SKIN, 4), loadout.getColors());
		assertEquals(JawIcon.BA_ATTACKER, loadout.getIcon());
		assertTrue(result.getSkippedLines().isEmpty());
	}

	@Test
	void skipsUnknownKeys()
	{
		LegacyFormat.ParseResult result = LegacyFormat.parse(ImmutableList.of("FOO:1", "FOO_KIT:2", "HEAD:1163"));
		assertEquals(ImmutableList.of("FOO:1", "FOO_KIT:2"), result.getSkippedLines());
		assertEquals(ImmutableMap.of(KitType.HEAD, 1163), result.getLoadout().getItems());
	}

	@Test
	void writesItemsWithNames()
	{
		Loadout loadout = new Loadout();
		loadout.getItems().put(KitType.HEAD, 1163);
		loadout.getItems().put(KitType.SHIELD, Loadout.NOTHING);
		List<String> lines = LegacyFormat.write(loadout, id -> "Item " + id);
		assertEquals(ImmutableList.of("SHIELD:-1 (Nothing)", "HEAD:1163 (Item 1163)"), lines);
	}

	@Test
	void writeThenParseKeepsLoadout()
	{
		Loadout loadout = new Loadout();
		loadout.getItems().put(KitType.HEAD, 1163);
		loadout.getItems().put(KitType.CAPE, Loadout.NOTHING);
		loadout.getKits().put(KitType.HAIR, 3);
		loadout.getKits().put(KitType.LEGS, 36);
		for (ColorType type : ColorType.values())
		{
			loadout.getColors().put(type, type.getColorables()[0].getColorId(type));
		}
		loadout.setIcon(JawIcon.SW_RED);

		List<String> lines = LegacyFormat.write(loadout, id -> "");
		assertEquals(loadout, LegacyFormat.parse(lines).getLoadout());
	}
}
