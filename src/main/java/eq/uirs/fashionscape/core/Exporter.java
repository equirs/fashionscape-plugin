package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;

/**
 * For saving/loading fashionscape to local files
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Exporter
{
	public static final File OUTFITS_DIR = new File(RuneLite.RUNELITE_DIR, "outfits");

	private static final Pattern PROFILE_PATTERN = Pattern.compile("^(\\w+):(-?\\d+).*");
	private static final String KIT_SUFFIX = "_KIT";
	private static final String COLOR_SUFFIX = "_COLOR";
	private static final String ICON_KEY = "ICON";

	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final ItemManager itemManager;

	private final SavedSwaps savedSwaps;
	private final Provider<FashionManager> fashionManagerProvider;

	// this should only be called from the client thread
	public List<String> stringifySwaps()
	{
		Set<Map.Entry<KitType, Integer>> itemEntries = new HashSet<>(savedSwaps.hiddenSlotEntries());
		itemEntries.addAll(savedSwaps.itemEntries());
		List<String> items = itemEntries.stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				int itemId = e.getValue();
				if (itemId == 0)
				{
					return slot.name() + ":-1 (Nothing)";
				}
				else
				{
					String itemName = itemManager.getItemComposition(itemId).getMembersName();
					return slot.name() + ":" + itemId + " (" + itemName + ")";
				}
			})
			.collect(Collectors.toList());
		List<String> kits = savedSwaps.kitEntries().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				Integer kitId = e.getValue();
				String kitName = FashionManager.KIT_ID_TO_KIT.get(kitId).getDisplayName();
				return slot.name() + KIT_SUFFIX + ":" + kitId + " (" + kitName + ")";
			})
			.collect(Collectors.toList());
		List<String> colors = savedSwaps.colorEntries().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				ColorType type = e.getKey();
				int colorId = e.getValue();
				return Arrays.stream(type.getColorables())
					.filter(c -> c.getColorId(type) == colorId)
					.findFirst()
					.map(c -> type.name() + COLOR_SUFFIX + ":" + colorId + " (" + c.getDisplayName() + ")")
					.orElse("");
			})
			.collect(Collectors.toList());
		List<String> icons = new ArrayList<>();
		JawIcon icon = savedSwaps.getSwappedIcon();
		if (savedSwaps.containsIcon())
		{
			icons.add(ICON_KEY + ":" + icon.getId() + " (" + icon.getDisplayName() + ")");
		}
		items.addAll(kits);
		items.addAll(colors);
		items.addAll(icons);
		return items;
	}

	public void parseImports(List<String> allLines)
	{
		Map<KitType, Integer> itemImports = new HashMap<>();
		Map<KitType, Integer> kitImports = new HashMap<>();
		Map<ColorType, Integer> colorImports = new HashMap<>();
		JawIcon icon = null;
		Set<KitType> removes = new HashSet<>();
		for (String line : allLines)
		{
			if (line.trim().isEmpty())
			{
				continue;
			}
			Matcher matcher = PROFILE_PATTERN.matcher(line);
			if (matcher.matches())
			{
				String slotStr = matcher.group(1);
				// could be item id, kit id, or color id
				int id = Integer.parseInt(matcher.group(2));
				KitType itemSlot = itemSlotMatch(slotStr);
				KitType kitSlot = kitSlotMatch(slotStr);
				ColorType colorType = colorSlotMatch(slotStr);
				if (itemSlot != null)
				{
					if (id <= 0)
					{
						removes.add(itemSlot);
					}
					else
					{
						itemImports.put(itemSlot, id);
					}
				}
				else if (kitSlot != null)
				{
					kitImports.put(kitSlot, id);
				}
				else if (colorType != null)
				{
					colorImports.put(colorType, id);
				}
				else if (slotStr.equals(ICON_KEY))
				{
					icon = JawIcon.fromId(id);
				}
				else
				{
					sendHighlightedMessage("Could not import line: " + line);
				}
			}
		}
		// if not explicitly included, items without data should be explicitly hidden
		FashionManager.ALLOWS_NOTHING.forEach(slot -> {
			if (!itemImports.containsKey(slot))
			{
				removes.add(slot);
			}
		});
		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty())
		{
			fashionManagerProvider.get().importSwaps(itemImports, kitImports, colorImports, icon, removes);
		}
	}

	@Nullable
	private KitType itemSlotMatch(String name)
	{
		try
		{
			return KitType.valueOf(name);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Nullable
	private KitType kitSlotMatch(String name)
	{
		try
		{
			String k = name.replace(KIT_SUFFIX, "");
			return KitType.valueOf(k);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Nullable
	private ColorType colorSlotMatch(String name)
	{
		try
		{
			String c = name.replace(COLOR_SUFFIX, "");
			return ColorType.valueOf(c);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public void importPlayer(PlayerComposition other)
	{
		int[] equipmentIds = other.getEquipmentIds();
		KitType[] slots = KitType.values();
		Map<KitType, Integer> equipIdImports = IntStream.range(0, equipmentIds.length).boxed()
			.collect(Collectors.toMap(i -> slots[i], i -> equipmentIds[i]));

		Map<KitType, Integer> itemImports = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() >= FashionManager.ITEM_OFFSET)
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - FashionManager.ITEM_OFFSET));
		Map<KitType, Integer> kitImports = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() >= FashionManager.KIT_OFFSET && e.getValue() < FashionManager.ITEM_OFFSET)
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() - FashionManager.KIT_OFFSET));
		Set<KitType> removals = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() <= 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		int[] colors = other.getColors();
		ColorType[] types = ColorType.values();
		Map<ColorType, Integer> colorImports = IntStream.range(0, colors.length).boxed()
			.collect(Collectors.toMap(i -> types[i], i -> colors[i]));

		JawIcon icon = null;
		Integer jawItemId = itemImports.get(KitType.JAW);
		if (jawItemId != null)
		{
			icon = JawKit.iconFromItemId(jawItemId);
			itemImports.remove(KitType.JAW);
		}

		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty() || !removals.isEmpty())
		{
			fashionManagerProvider.get().importSwaps(itemImports, kitImports, colorImports, icon, removals);
		}
	}

	public void export(File selected)
	{
		clientThread.invokeLater(() -> {
			try (PrintWriter out = new PrintWriter(selected))
			{
				List<String> exports = stringifySwaps();
				for (String line : exports)
				{
					out.println(line);
				}
				sendHighlightedMessage("Saved fashionscape to " + selected.getName());
			}
			catch (FileNotFoundException e)
			{
				log.warn("Could not find selected file for swaps export", e);
			}
		});
	}

	private void sendHighlightedMessage(String message)
	{
		String chatMessage = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(message)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(chatMessage)
			.build());
	}
}
