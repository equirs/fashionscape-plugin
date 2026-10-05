package eq.uirs.fashionscape.core.loadout;

import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.runelite.api.kit.KitType;

/**
 * Reads line-based text format from older saved files (e.g. "HEAD:1163 (Rune full helm)").
 * Text after the id is for human readability, the parser ignores it.
 */
public final class LegacyFormat
{
	private static final Pattern LINE_PATTERN = Pattern.compile("^(\\w+):(-?\\d+).*");
	private static final String KIT_SUFFIX = "_KIT";
	private static final String COLOR_SUFFIX = "_COLOR";
	private static final String ICON_KEY = "ICON";

	private LegacyFormat()
	{
	}

	// lines with an unknown key are skipped
	public static Loadout parse(List<String> lines)
	{
		Loadout loadout = new Loadout();
		for (String line : lines)
		{
			Matcher matcher = LINE_PATTERN.matcher(line.trim());
			if (!matcher.matches())
			{
				continue;
			}
			String key = matcher.group(1);
			// could be item id, kit id, or color id
			int id = Integer.parseInt(matcher.group(2));
			KitType itemSlot = enumOrNull(KitType.class, key);
			KitType kitSlot = key.endsWith(KIT_SUFFIX) ? enumOrNull(KitType.class, strip(key, KIT_SUFFIX)) : null;
			ColorType colorType = key.endsWith(COLOR_SUFFIX) ? enumOrNull(ColorType.class, strip(key, COLOR_SUFFIX)) : null;
			if (itemSlot != null)
			{
				loadout.getItems().put(itemSlot, id > 0 ? id : Loadout.NOTHING);
			}
			else if (kitSlot != null)
			{
				loadout.getKits().put(kitSlot, id);
			}
			else if (colorType != null)
			{
				loadout.getColors().put(colorType, id);
			}
			else if (key.equals(ICON_KEY))
			{
				loadout.setIcon(JawIcon.fromId(id));
			}
		}
		return loadout;
	}

	private static String strip(String key, String suffix)
	{
		return key.substring(0, key.length() - suffix.length());
	}

	@Nullable
	private static <E extends Enum<E>> E enumOrNull(Class<E> type, String name)
	{
		try
		{
			return Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
