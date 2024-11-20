package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
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
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Deals with saving/loading fashionscape to local files
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Exporter
{
	public static final File OUTFITS_DIR = new File(RuneLite.RUNELITE_DIR, "outfits");
	public static final Pattern PROFILE_PATTERN = Pattern.compile("^(\\w+):(-?\\d+).*");

	private static final String KIT_SUFFIX = "_KIT";
	private static final String COLOR_SUFFIX = "_COLOR";
	private static final String ICON_KEY = "ICON";

	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final ItemManager itemManager;

	private final Layers layers;
	private final Locks locks;
	private final History history;

	public void parseImports(List<String> allLines)
	{
		Map<KitType, Integer> itemImports = new HashMap<>();
		Map<KitType, Integer> kitImports = new HashMap<>();
		Map<ColorType, Integer> colorImports = new HashMap<>();
		JawIcon icon = null;
		Set<KitType> nothings = new HashSet<>();
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
						nothings.add(itemSlot);
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
		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty())
		{
			performImport(itemImports, kitImports, colorImports, icon, nothings);
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
		Set<KitType> nothingSlots = equipIdImports.entrySet().stream()
			.filter(e -> e.getValue() < FashionManager.KIT_OFFSET && FashionManager.ALLOWS_NOTHING_ITEMS.contains(e.getKey()))
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

		if (!itemImports.isEmpty() || !kitImports.isEmpty() || !colorImports.isEmpty() || !nothingSlots.isEmpty())
		{
			performImport(itemImports, kitImports, colorImports, icon, nothingSlots);
		}
	}

	/**
	 * Loads all given params into layers. This clears the user's locks.
	 *
	 * @param newItems     incoming item ids
	 * @param newKits      incoming kit ids
	 * @param newColors    incoming color ids
	 * @param icon         jaw icon (nullable)
	 * @param nothingSlots slots that should be set to "nothing" directly
	 */
	public void performImport(
		Map<KitType, Integer> newItems,
		Map<KitType, Integer> newKits,
		Map<ColorType, Integer> newColors,
		JawIcon icon,
		Set<KitType> nothingSlots)
	{
		clientThread.invokeLater(() -> {
			locks.clear();

			Diff diff = Diff.empty();

			// don't set to "nothing" if item already hides
			Set<KitType> remainingNothingSlots = new HashSet<>(nothingSlots);

			// track slots that haven't changed (will be unset later)
			Set<KitType> unsetSlots = new HashSet<>(Arrays.asList(KitType.values()));

			// import items onto player
			List<SlotInfo> items = newItems.entrySet().stream()
				.map(e -> SlotInfo.lookUp(e.getValue() + FashionManager.ITEM_OFFSET, e.getKey()))
				.collect(Collectors.toList());
			for (SlotInfo item : items)
			{
				diff = Diff.merge(layers.set(item.getSlot(), item, false), diff);
				unsetSlots.remove(item.getSlot());
				item.getHidden().forEach(i -> {
					unsetSlots.remove(i);
					remainingNothingSlots.remove(i);
				});
			}

			// import icon
			diff = Diff.merge(layers.setIcon(icon, false), diff);

			// import kits (requires known gender)
			Integer gender = layers.getGender();
			if (gender != null)
			{
				List<SlotInfo> kits = newKits.entrySet().stream()
					.map(e -> {
						KitType slot = e.getKey();
						Kit kit = KitUtil.getWithAnalog(e.getValue(), gender);
						// if no analog exists, use a fallback (don't notify UI that the player's slot is unknown)
						if (kit == null)
						{
							int kitId = Fallbacks.getKit(slot, gender, false);
							kit = KitUtil.getWithAnalog(kitId, gender);
						}
						return kit != null ? SlotInfo.kit(kit, gender) : SlotInfo.nothing(slot);
					})
					.filter(s -> !s.isNothing())
					.collect(Collectors.toList());
				for (SlotInfo kit : kits)
				{
					diff = Diff.merge(layers.set(kit.getSlot(), kit, false), diff);
					unsetSlots.remove(kit.getSlot());
				}
			}
			else if (!newKits.isEmpty())
			{
				sendHighlightedMessage("Not all imports could be loaded: can't determine your character's gender");
			}

			// set "nothing" where needed
			for (KitType slot : remainingNothingSlots)
			{
				diff = Diff.merge(layers.set(slot, SlotInfo.nothing(slot), false), diff);
				unsetSlots.remove(slot);
			}

			// unset any slot that hasn't been touched at all
			for (KitType slot : unsetSlots)
			{
				diff = Diff.merge(layers.set(slot, null, false), diff);
			}

			// finally, set/unset color ids
			for (ColorType type : ColorType.values())
			{
				diff = Diff.merge(layers.setColor(type, newColors.get(type), false), diff);
			}

			history.append(diff);
		});
	}

	public void export(File selected)
	{
		clientThread.invokeLater(() -> {
			try (PrintWriter out = new PrintWriter(selected))
			{
				List<String> exports = getExportsAsStrings();
				for (String line : exports)
				{
					out.println(line);
				}
				sendHighlightedMessage("Saved fashionscape to " + selected.getName());
			}
			catch (FileNotFoundException e)
			{
				log.warn("Could not find selected file for fashionscape export", e);
			}
		});
	}

	// this should only be called from the client thread
	@VisibleForTesting
	List<String> getExportsAsStrings()
	{
		ModelInfo modelInfo = layers.getVirtualModels();
		Set<Map.Entry<KitType, Integer>> itemEntries = modelInfo.getItems().getAll().entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, i -> i.getValue().getItemId()))
			.entrySet();
		List<String> result = itemEntries.stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				int itemId = e.getValue();
				String prefix = slot.name() + ":";
				if (itemId < 0)
				{
					return prefix + "-1 (Nothing)";
				}
				else
				{
					String itemName = itemManager.getItemComposition(itemId).getMembersName();
					return prefix + itemId + " (" + itemName + ")";
				}
			})
			.collect(Collectors.toList());
		List<String> kits = modelInfo.getKits().getAll().entrySet().stream()
			.sorted(Comparator.comparingInt(Map.Entry::getValue))
			.map(e -> {
				KitType slot = e.getKey();
				Integer kitId = e.getValue();
				String kitName = KitUtil.KIT_ID_TO_KIT.get(kitId).getDisplayName();
				return slot.name() + KIT_SUFFIX + ":" + kitId + " (" + kitName + ")";
			})
			.collect(Collectors.toList());
		List<String> colors = modelInfo.getColors().getAll().entrySet().stream()
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
		JawIcon icon = modelInfo.getIcon();
		if (icon != null)
		{
			icons.add(ICON_KEY + ":" + icon.getId() + " (" + icon.getDisplayName() + ")");
		}
		result.addAll(kits);
		result.addAll(colors);
		result.addAll(icons);
		return result;
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
