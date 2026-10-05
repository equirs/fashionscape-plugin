package eq.uirs.fashionscape.core.loadout;

import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.Kit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.kit.KitType;

/**
 * Reads / writes line-based text format from older saved files (e.g. "HEAD:1163 (Rune full helm)").
 * Text after the id is for human readability, the parser ignores it.
 */
public final class LegacyFormat
{
	private static final Pattern LINE_PATTERN = Pattern.compile("^(\\w+):(-?\\d+).*");
	private static final String KIT_SUFFIX = "_KIT";
	private static final String COLOR_SUFFIX = "_COLOR";
	private static final String ICON_KEY = "ICON";

	@Value
	public static class ParseResult
	{
		Loadout loadout;
		// lines that look like entries but have an unknown key
		List<String> skippedLines;
	}

	private LegacyFormat()
	{
	}

	public static ParseResult parse(List<String> lines)
	{
		Loadout loadout = new Loadout();
		List<String> skipped = new ArrayList<>();
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
			else
			{
				skipped.add(line);
			}
		}
		return new ParseResult(loadout, skipped);
	}


	// One line per entry, grouped by items, kits, colors, then icon
	public static List<String> write(Loadout loadout, IntFunction<String> itemNames)
	{
		List<String> result = new ArrayList<>();
		sortedByValue(loadout.getItems()).forEach(e -> {
			String prefix = e.getKey().name() + ":";
			int itemId = e.getValue();
			result.add(itemId < 0 ? prefix + Loadout.NOTHING + " (Nothing)" :
				prefix + itemId + " (" + itemNames.apply(itemId) + ")");
		});
		sortedByValue(loadout.getKits()).forEach(e -> {
			Kit kit = KitUtil.KIT_ID_TO_KIT.get(e.getValue());
			String suffix = kit != null ? " (" + kit.getDisplayName() + ")" : "";
			result.add(e.getKey().name() + KIT_SUFFIX + ":" + e.getValue() + suffix);
		});
		sortedByValue(loadout.getColors()).forEach(e -> {
			ColorType type = e.getKey();
			int colorId = e.getValue();
			Arrays.stream(type.getColorables())
				.filter(c -> c.getColorId(type) == colorId)
				.findFirst()
				.ifPresent(c -> result.add(type.name() + COLOR_SUFFIX + ":" + colorId + " (" + c.getDisplayName() + ")"));
		});
		JawIcon icon = loadout.getIcon();
		if (icon != null)
		{
			result.add(ICON_KEY + ":" + icon.getId() + " (" + icon.getDisplayName() + ")");
		}
		return result;
	}

	private static <K> List<Map.Entry<K, Integer>> sortedByValue(Map<K, Integer> map)
	{
		return map.entrySet().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.collect(Collectors.toList());
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
