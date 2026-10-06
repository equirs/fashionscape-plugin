package eq.uirs.fashionscape.core.loadout;

import com.google.common.annotations.VisibleForTesting;
import eq.uirs.fashionscape.core.Diff;
import eq.uirs.fashionscape.core.Fallbacks;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.History;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

/**
 * Converts between loadouts and the player's virtual models
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LoadoutManager
{
	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;

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
			layers.resetPreview();
			locks.clear();
			history.append(write(loadout, false));
		});
	}

	/**
	 * Shows the loadout in place of the virtual models until the preview is reset. Ignores locks, like applying does.
	 */
	public void preview(Loadout loadout)
	{
		clientThread.invokeLater(() -> {
			layers.resetPreview();
			layers.setPreviewReplacesVirtual(true);
			write(loadout, true);
		});
	}

	/**
	 * Ends a preview. Queued like {@link #preview}, so it can't run before a preview that's still pending.
	 */
	public void endPreview()
	{
		clientThread.invokeLater(layers::resetPreview);
	}

	// sets every slot, so slots the loadout doesn't mention are unset
	private Diff write(Loadout loadout, boolean isPreview)
	{
		Diff diff = Diff.empty();
		Map<KitType, Integer> items = resolveItems(loadout);

		// track slots that haven't changed (will be unset later)
		Set<KitType> unsetSlots = new HashSet<>(Arrays.asList(KitType.values()));

		// import items onto player
		for (Map.Entry<KitType, Integer> entry : items.entrySet())
		{
			if (entry.getValue() < 0)
			{
				continue;
			}
			SlotInfo item = SlotInfo.lookUp(entry.getValue() + FashionManager.ITEM_OFFSET, entry.getKey());
			diff = Diff.merge(layers.set(item.getSlot(), item, isPreview), diff);
			unsetSlots.remove(item.getSlot());
			item.getHidden().forEach(unsetSlots::remove);
		}

		// import icon
		diff = Diff.merge(layers.setIcon(loadout.getIcon(), isPreview), diff);

		// import kits (requires known gender)
		Integer gender = layers.getGender();
		if (gender != null)
		{
			for (Map.Entry<KitType, Integer> entry : resolveKits(loadout, gender).entrySet())
			{
				KitType slot = entry.getKey();
				diff = Diff.merge(layers.set(slot, SlotInfo.kit(entry.getValue(), slot), isPreview), diff);
				unsetSlots.remove(slot);
			}
		}
		else if (!loadout.getKits().isEmpty() && !isPreview)
		{
			sendHighlightedMessage("Not all imports could be loaded: can't determine your character's gender");
		}

		// set "nothing" where needed
		for (Map.Entry<KitType, Integer> entry : items.entrySet())
		{
			if (entry.getValue() < 0)
			{
				KitType slot = entry.getKey();
				diff = Diff.merge(layers.set(slot, SlotInfo.nothing(slot), isPreview), diff);
				unsetSlots.remove(slot);
			}
		}

		// unset any slot that hasn't been touched
		for (KitType slot : unsetSlots)
		{
			diff = Diff.merge(layers.set(slot, null, isPreview), diff);
		}

		// finally, set/unset color ids
		for (ColorType type : ColorType.values())
		{
			diff = Diff.merge(layers.setColor(type, loadout.getColors().get(type), isPreview), diff);
		}
		return diff;
	}


	/**
	 * Returns whether applying the loadout would leave the current look unchanged. Call on the client thread.
	 */
	public boolean isApplied(Loadout loadout)
	{
		Loadout look = capture();
		return look.getItems().equals(resolveItems(loadout)) &&
			look.getColors().equals(loadout.getColors()) &&
			Objects.equals(look.getIcon(), loadout.getIcon()) &&
			look.getKits().equals(resolveKits(loadout, layers.getGender()));
	}

	/**
	 * Resolves loadout's item ids, skipping "nothing" in slots other items hide (e.g. nothing shield + 2h weapon).
	 * Older versions saved loadouts like these.
	 */
	@VisibleForTesting
	Map<KitType, Integer> resolveItems(Loadout loadout)
	{
		Set<KitType> hidden = loadout.getItems().entrySet().stream()
			.filter(e -> e.getValue() >= 0)
			.flatMap(e -> SlotInfo.lookUp(e.getValue() + FashionManager.ITEM_OFFSET, e.getKey()).getHidden().stream())
			.collect(Collectors.toSet());
		Map<KitType, Integer> result = new HashMap<>(loadout.getItems());
		result.entrySet().removeIf(e -> e.getValue() < 0 && hidden.contains(e.getKey()));
		return result;
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
