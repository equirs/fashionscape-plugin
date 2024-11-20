package eq.uirs.fashionscape.core.randomizer;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.Diff;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.ItemSlotUtil;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.core.utils.ListUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import org.jetbrains.annotations.VisibleForTesting;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Randomizer
{
	private final Layers layers;
	private final Locks locks;
	private final FashionscapeConfig config;
	private final ColorScorer colorScorer;
	private final Client client;
	private final ItemManager itemManager;
	private final Exclusions exclusions;

	private final Map<KitType, List<SlotInfo>> slotToItemsMemo = new HashMap<>();
	private final Random random = new Random();

	@VisibleForTesting
	void setRandomSeed(long seed)
	{
		random.setSeed(seed);
	}

	public Diff shuffle()
	{
		if (slotToItemsMemo.isEmpty())
		{
			repopulateMemo();
		}

		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		if (intelligence != RandomizerIntelligence.NONE)
		{
			computeLockedColorScores();
		}
		Diff diff = Diff.empty();
		// first, try to occupy all unlocked slots with items
		List<SlotInfo> slottedItems = findItems();
		for (SlotInfo item : slottedItems)
		{
			diff = Diff.merge(layers.set(item.getSlot(), item, false), diff);
		}

		// next, try to occupy remaining slots with random kits (no color scoring since they're recolor-able)
		List<SlotInfo> slottedKits = findKits(slottedItems);
		for (SlotInfo kit : slottedKits)
		{
			diff = Diff.merge(layers.set(kit.getSlot(), kit, false), diff);
		}

		// find the icon that most closely matches on color
		JawIcon icon = findIcon();
		if (icon != null)
		{
			diff = Diff.merge(layers.setIcon(icon, false), diff);
		}

		// lastly, shuffle the player's base model colors
		Map<ColorType, Colorable> colors = findColors();
		for (Map.Entry<ColorType, Colorable> entry : colors.entrySet())
		{
			ColorType colorType = entry.getKey();
			Colorable color = entry.getValue();
			diff = Diff.merge(layers.setColor(colorType, color.getColorId(colorType), false), diff);
		}
		return diff;
	}

	public void repopulateMemo()
	{
		slotToItemsMemo.clear();
		Set<Integer> skips = exclusions.getAll();
		for (int i = 0; i < client.getItemCount(); i++)
		{
			int canonical = itemManager.canonicalize(i);
			// can skip banknote/placeholder items since the canonical version will come up eventually
			if (i != canonical || skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(i);
			int itemId = itemComposition.getId();
			KitType slot = ItemSlotUtil.getSlot(itemId, itemManager);
			if (slot == null)
			{
				continue;
			}
			SlotInfo slotInfo = SlotInfo.lookUp(itemId + FashionManager.ITEM_OFFSET, slot);
			List<SlotInfo> itemsInSlot = slotToItemsMemo.getOrDefault(slot, new ArrayList<>());
			itemsInSlot.add(slotInfo);
			slotToItemsMemo.put(slot, itemsInSlot);
		}
	}

	private void computeLockedColorScores()
	{
		ModelInfo info = layers.getVirtualModels();
		Map<KitType, Integer> lockedItems = info.getItems().getAll().values().stream()
			.filter(i -> locks.contains(i.getSlot()))
			.collect(Collectors.toMap(SlotInfo::getSlot, SlotInfo::getItemId));
		if (locks.isIcon() && info.getIcon() != null && info.getIcon() != JawIcon.NOTHING)
		{
			// consider only the icon part of the model, not the built-in facial hair
			Integer iconItemId = JawKit.NO_JAW.getIconItemId(info.getIcon());
			if (iconItemId != null)
			{
				lockedItems.put(KitType.JAW, iconItemId);
			}
		}
		Map<ColorType, Colorable> lockedColors = info.getColors().getAllColorable().entrySet().stream()
			.filter(e -> locks.getColor(e.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		colorScorer.setPlayerInfo(lockedItems, lockedColors);
	}

	@VisibleForTesting
	List<SlotInfo> findItems()
	{
		List<KitType> remainingItemSlots = Arrays.stream(KitType.values())
			.filter(s -> !locks.contains(s))
			.collect(Collectors.toList());
		Collections.shuffle(remainingItemSlots, random);
		List<SlotInfo> result = new ArrayList<>();
		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		while (!remainingItemSlots.isEmpty())
		{
			KitType slot = remainingItemSlots.remove(0);
			// skip jaw items, as these are handled later as icons
			if (slot == KitType.JAW)
			{
				continue;
			}
			// items in this slot cannot conflict with locked or already-filled slots
			Set<KitType> remainingSet = new HashSet<>(remainingItemSlots);
			List<SlotInfo> allCandidates = slotToItemsMemo.getOrDefault(slot, new ArrayList<>()).stream()
				.filter(slotInfo -> locks.isAllowed(slot, slotInfo) &&
					remainingSet.containsAll(slotInfo.getHidden()))
				.collect(Collectors.toList());
			if (!allCandidates.isEmpty())
			{
				List<SlotInfo> candidates = ListUtil.takeRandomSample(allCandidates, intelligence.getDepth(), random);
				int multiplier = intelligence == RandomizerIntelligence.CURSED ? -1 : 1;
				SlotInfo bestMatch = candidates.stream()
					.max(Comparator.comparingDouble(c -> multiplier * colorScorer.score(c.getItemId(), c.getSlot())))
					.orElse(candidates.get(0));
				result.add(bestMatch);
				// also remove slots that the match hides
				bestMatch.getHidden().forEach(remainingItemSlots::remove);
				colorScorer.addPlayerInfo(bestMatch.getSlot(), bestMatch.getItemId());
			}
		}
		return result;
	}

	@VisibleForTesting
	List<SlotInfo> findKits(List<SlotInfo> existingItems)
	{
		List<SlotInfo> result = new ArrayList<>();
		Integer gender = layers.getGender();
		if (gender != null && !config.excludeBaseModels())
		{
			// check locks and items from last step to ensure kit model can be shown
			List<KitType> remainingKitSlots = Arrays.stream(KitType.values())
				// kit id doesn't matter, just need to check if kit is allowed for current locks
				.filter(slot -> locks.isAllowed(slot, SlotInfo.kit(0, slot)) &&
					existingItems.stream().noneMatch(item -> item.getSlot() == slot || item.hides(slot)))
				.collect(Collectors.toList());
			Collections.shuffle(remainingKitSlots, random);
			while (!remainingKitSlots.isEmpty())
			{
				KitType slot = remainingKitSlots.remove(0);
				// only consider kits applicable to current gender
				List<SlotInfo> candidates = KitUtil.KIT_TYPE_TO_KITS.getOrDefault(slot, new ArrayList<>()).stream()
					.filter(k -> !k.isHidden() && k.getKitId(gender) != null && k != JawKit.NO_JAW)
					.map(k -> SlotInfo.kit(k, gender))
					.collect(Collectors.toList());
				if (!candidates.isEmpty())
				{
					result.add(candidates.get(random.nextInt(candidates.size())));
				}
			}
		}
		return result;
	}

	@VisibleForTesting
	JawIcon findIcon()
	{
		if (locks.isIcon() || config.excludeBaseModels())
		{
			return null;
		}
		JawIcon icon = JawIcon.NOTHING;
		List<JawIcon> icons = Arrays.asList(JawIcon.values());
		// score colors if intelligence is not minimal
		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		if (intelligence != RandomizerIntelligence.NONE && intelligence != RandomizerIntelligence.CURSED)
		{
			Map<JawIcon, Double> scores = icons.stream()
				.collect(Collectors.toMap(i -> i, i -> {
					Integer itemId = JawKit.NO_JAW.getIconItemId(i);
					return itemId != null ? colorScorer.score(itemId, null) : 0;
				}));
			// use icon if >75% match
			icon = scores.entrySet().stream()
				.filter(e -> e.getValue() > 0.75)
				.max(Comparator.comparingDouble(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(JawIcon.NOTHING);
		}
		else if (random.nextDouble() > 0.33)
		{
			// since there aren't many icons, just prefer not showing one most of the time
			icon = icons.get(random.nextInt(icons.size()));
		}
		return icon;
	}

	@VisibleForTesting
	Map<ColorType, Colorable> findColors()
	{
		Map<ColorType, Colorable> result = new HashMap<>();
		if (config.excludeBaseModels())
		{
			return result;
		}
		for (ColorType colorType : ColorType.values())
		{
			if (locks.getColor(colorType))
			{
				continue;
			}
			List<Colorable> colorables = Arrays.asList(colorType.getColorables().clone());
			Collections.shuffle(colorables, random);
			int limit;
			RandomizerIntelligence intelligence = config.randomizerIntelligence();
			switch (intelligence)
			{
				case LOW:
					limit = Math.max(1, colorables.size() / 4);
					break;
				case MODERATE:
				case CURSED:
					limit = Math.max(1, colorables.size() / 2);
					break;
				case HIGH:
					limit = colorables.size();
					break;
				default:
					limit = 1;
			}
			int multiplier = intelligence == RandomizerIntelligence.CURSED ? -1 : 1;
			Colorable best = colorables.stream()
				.limit(limit)
				.max(Comparator.comparingDouble(c -> colorScorer.score(c, colorType) * multiplier))
				.orElse(colorables.get(0));
			result.put(colorType, best);
			colorScorer.addPlayerColor(colorType, best);
		}
		return result;
	}
}
