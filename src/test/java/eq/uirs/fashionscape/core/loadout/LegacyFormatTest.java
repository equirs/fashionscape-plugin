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
		Loadout loadout = LegacyFormat.parse(lines);
		assertEquals(ImmutableMap.of(KitType.HEAD, 1163, KitType.SHIELD, Loadout.NOTHING), loadout.getItems());
		assertEquals(ImmutableMap.of(KitType.HAIR, 3), loadout.getKits());
		assertEquals(ImmutableMap.of(ColorType.SKIN, 4), loadout.getColors());
		assertEquals(JawIcon.BA_ATTACKER, loadout.getIcon());
	}

	@Test
	void skipsUnknownKeys()
	{
		Loadout loadout = LegacyFormat.parse(ImmutableList.of("FOO:1", "FOO_KIT:2", "HEAD:1163"));
		assertEquals(ImmutableMap.of(KitType.HEAD, 1163), loadout.getItems());
		assertTrue(loadout.getKits().isEmpty());
	}
}
