package eq.uirs.fashionscape.core;

import com.google.common.annotations.VisibleForTesting;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.loadout.LegacyFormat;
import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

/**
 * Converts between loadouts and the player's virtual models
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LoadoutManager
{
	public static final File OUTFITS_DIR = new File(RuneLite.RUNELITE_DIR, "outfits");

	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final ItemManager itemManager;

	private final Layers layers;
	private final Locks locks;
	private final History history;
	private final Fallbacks fallbacks;

	/**
	 * Loadout of current virtual models. Call only on client thread.
	 */
	public Loadout capture()
	{
		ModelInfo models = layers.getVirtualModels();
		Loadout loadout = new Loadout();
		models.getItems().getAll().forEach((slot, info) ->
			loadout.getItems().put(slot, info.isItem() ? info.getItemId() : Loadout.NOTHING));
		loadout.getKits().putAll(models.getKits().getAll());
		loadout.getColors().putAll(models.getColors().getAll());
		loadout.setIcon(models.getIcon());
		return loadout;
	}

	public void importPlayer(PlayerComposition other)
	{
		Loadout loadout = new Loadout();
		int[] equipmentIds = other.getEquipmentIds();
		KitType[] slots = KitType.values();
		for (int i = 0; i < equipmentIds.length; i++)
		{
			KitType slot = slots[i];
			int equipId = equipmentIds[i];
			if (equipId >= FashionManager.ITEM_OFFSET)
			{
				loadout.getItems().put(slot, equipId - FashionManager.ITEM_OFFSET);
			}
			else if (equipId >= FashionManager.KIT_OFFSET)
			{
				loadout.getKits().put(slot, equipId - FashionManager.KIT_OFFSET);
			}
			else if (FashionManager.ALLOWS_NOTHING_ITEMS.contains(slot))
			{
				loadout.getItems().put(slot, Loadout.NOTHING);
			}
		}

		int[] colors = other.getColors();
		ColorType[] types = ColorType.values();
		for (int i = 0; i < colors.length; i++)
		{
			loadout.getColors().put(types[i], colors[i]);
		}

		Integer jawItemId = loadout.getItems().remove(KitType.JAW);
		if (jawItemId != null)
		{
			loadout.setIcon(JawKit.iconFromItemId(jawItemId));
		}

		if (!loadout.isEmpty())
		{
			apply(loadout);
		}
	}

	/**
	 * Replaces virtual models with this loadout. Clears the user's locks.
	 */
	public void apply(Loadout loadout)
	{
		clientThread.invokeLater(() -> {
			locks.clear();

			Diff diff = Diff.empty();

			// don't set to "nothing" if item already hides
			Set<KitType> remainingNothingSlots = loadout.getItems().entrySet().stream()
				.filter(e -> e.getValue() < 0)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

			// track slots that haven't changed (will be unset later)
			Set<KitType> unsetSlots = new HashSet<>(Arrays.asList(KitType.values()));

			// import items onto player
			for (Map.Entry<KitType, Integer> entry : loadout.getItems().entrySet())
			{
				if (entry.getValue() < 0)
				{
					continue;
				}
				SlotInfo item = SlotInfo.lookUp(entry.getValue() + FashionManager.ITEM_OFFSET, entry.getKey());
				diff = Diff.merge(layers.set(item.getSlot(), item, false), diff);
				unsetSlots.remove(item.getSlot());
				item.getHidden().forEach(s -> {
					unsetSlots.remove(s);
					remainingNothingSlots.remove(s);
				});
			}

			// import icon
			diff = Diff.merge(layers.setIcon(loadout.getIcon(), false), diff);

			// import kits (requires known gender)
			Integer gender = layers.getGender();
			if (gender != null)
			{
				for (Map.Entry<KitType, Integer> entry : resolveKits(loadout, gender).entrySet())
				{
					KitType slot = entry.getKey();
					diff = Diff.merge(layers.set(slot, SlotInfo.kit(entry.getValue(), slot), false), diff);
					unsetSlots.remove(slot);
				}
			}
			else if (!loadout.getKits().isEmpty())
			{
				sendHighlightedMessage("Not all imports could be loaded: can't determine your character's gender");
			}

			// set "nothing" where needed
			for (KitType slot : remainingNothingSlots)
			{
				diff = Diff.merge(layers.set(slot, SlotInfo.nothing(slot), false), diff);
				unsetSlots.remove(slot);
			}

			// unset any slot that hasn't been touched
			for (KitType slot : unsetSlots)
			{
				diff = Diff.merge(layers.set(slot, null, false), diff);
			}

			// finally, set/unset color ids
			for (ColorType type : ColorType.values())
			{
				diff = Diff.merge(layers.setColor(type, loadout.getColors().get(type), false), diff);
			}

			history.append(diff);
		});
	}

	/**
	 * Returns whether applying the loadout would leave the current look unchanged. Call on the client thread.
	 */
	public boolean isApplied(Loadout loadout)
	{
		Loadout look = capture();
		return look.getItems().equals(loadout.getItems()) &&
			look.getColors().equals(loadout.getColors()) &&
			Objects.equals(look.getIcon(), loadout.getIcon()) &&
			look.getKits().equals(resolveKits(loadout, layers.getGender()));
	}

	/**
	 * Resolves loadout's kit ids for the current gender, empty if gender is unknown.
	 */
	@VisibleForTesting
	Map<KitType, Integer> resolveKits(Loadout loadout, @Nullable Integer gender)
	{
		Map<KitType, Integer> result = new HashMap<>();
		if (gender == null)
		{
			return result;
		}
		loadout.getKits().forEach((slot, kitId) -> {
			Kit kit = KitUtil.KIT_ID_TO_KIT.get(kitId);
			// if no analog exists, use a fallback (don't notify UI that the player's slot is unknown)
			if (kit == null)
			{
				kit = KitUtil.KIT_ID_TO_KIT.get(fallbacks.getKit(slot, gender, false));
			}
			Integer genderedId = kit != null ? kit.getKitId(gender) : null;
			if (genderedId != null)
			{
				result.put(kit.getKitType(), genderedId);
			}
		});
		return result;
	}

	public void importLegacy(List<String> lines)
	{
		LegacyFormat.ParseResult result = LegacyFormat.parse(lines);
		result.getSkippedLines().forEach(line -> sendHighlightedMessage("Could not import line: " + line));
		if (!result.getLoadout().isEmpty())
		{
			apply(result.getLoadout());
		}
	}

	public void exportLegacy(File selected)
	{
		clientThread.invokeLater(() -> {
			try (PrintWriter out = new PrintWriter(selected))
			{
				List<String> lines = LegacyFormat.write(capture(),
					itemId -> itemManager.getItemComposition(itemId).getMembersName());
				lines.forEach(out::println);
				sendHighlightedMessage("Saved fashionscape to " + selected.getName());
			}
			catch (FileNotFoundException e)
			{
				log.warn("Could not find selected file for fashionscape export", e);
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
