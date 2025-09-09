package eq.uirs.fashionscape.core.randomizer;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.FashionscapePlugin;
import eq.uirs.fashionscape.colors.ColorScorer;
import eq.uirs.fashionscape.core.CompoundSwap;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SavedSwaps;
import eq.uirs.fashionscape.core.SwapDiff;
import eq.uirs.fashionscape.core.SwapMode;
import eq.uirs.fashionscape.data.anim.ItemInteractions;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Randomizer
{
	private static final Map<KitType, List<Kit>> KIT_TYPE_TO_KITS = Arrays.stream(KitType.values())
		.collect(Collectors.toMap(s -> s, s -> Arrays.asList(Kit.allInSlot(s, true))));;

	private final Client client;
	private final ItemManager itemManager;
	private final FashionscapeConfig config;
	private final ColorScorer colorScorer;
	private final SavedSwaps savedSwaps;
	private final Provider<FashionManager> fashionManagerProvider;

	@Value
	private static class Candidate
	{
		public int itemId;
		public KitType slot;
	}

	/**
	 * Randomizes items/kits/colors in unlocked slots.
	 * Can only be called from the client thread.
	 */
	public void shuffle()
	{
		FashionManager fashionManager = fashionManagerProvider.get();
		final Random r = new Random();
		RandomizerIntelligence intelligence = config.randomizerIntelligence();
		int size = intelligence.getDepth();
		if (size > 1)
		{
			Map<KitType, Integer> lockedItems = Arrays.stream(KitType.values())
				.filter(s -> savedSwaps.isItemLocked(s) && savedSwaps.containsItem(s))
				.collect(Collectors.toMap(s -> s, savedSwaps::getItem));
			if (savedSwaps.isIconLocked() && savedSwaps.containsIcon())
			{
				Integer iconItemId = JawKit.NO_JAW.getIconItemId(savedSwaps.getSwappedIcon());
				if (iconItemId != null)
				{
					lockedItems.put(KitType.JAW, iconItemId);
				}
			}
			Map<ColorType, Colorable> lockedColors = fashionManager.swappedColorsMap().entrySet().stream()
				.filter(e -> savedSwaps.isColorLocked(e.getKey()) && savedSwaps.containsColor(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			colorScorer.setPlayerInfo(lockedItems, lockedColors);
		}
		Map<KitType, Boolean> itemSlotsToRevert = Arrays.stream(KitType.values())
			.collect(Collectors.toMap(slot -> slot, savedSwaps::isItemLocked));

		// pre-fill slots that will be skipped with -1 as a placeholder
		Map<KitType, Integer> newSwaps = Arrays.stream(KitType.values())
			.filter(itemSlotsToRevert::get)
			.collect(Collectors.toMap(s -> s, s -> -1));
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
		List<Candidate> candidates = new ArrayList<>(size);
		List<Integer> randomOrder = IntStream.range(0, client.getItemCount()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomOrder);
		Iterator<Integer> randomIterator = randomOrder.iterator();
		while (newSwaps.size() < KitType.values().length && randomIterator.hasNext())
		{
			Integer i = randomIterator.next();
			int canonical = itemManager.canonicalize(i);
			if (skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(canonical);
			int itemId = itemComposition.getId();
			KitType slot = fashionManager.slotForId(fashionManager.slotIdFor(itemComposition));
			if (slot != null && !newSwaps.containsKey(slot))
			{
				// Don't equip a 2h weapon if we already have a shield
				if (slot == KitType.WEAPON)
				{
					ItemEquipmentStats stats = fashionManager.equipmentStatsFor(itemId);
					if (stats != null && stats.isTwoHanded() && newSwaps.get(KitType.SHIELD) != null)
					{
						continue;
					}
				}
				// Don't equip a shield if we already have a 2h weapon (mark shield as removed instead)
				else if (slot == KitType.SHIELD)
				{
					Integer weaponItemId = newSwaps.get(KitType.WEAPON);
					if (weaponItemId != null)
					{
						ItemEquipmentStats stats = fashionManager.equipmentStatsFor(weaponItemId);
						if (stats != null && stats.isTwoHanded())
						{
							newSwaps.put(KitType.SHIELD, -1);
							continue;
						}
					}
				}
				else if (slot == KitType.HEAD)
				{
					// Don't equip a helm if it hides hair and hair is locked
					if (!ItemInteractions.HAIR_HELMS.contains(itemId) && savedSwaps.isKitLocked(KitType.HAIR))
					{
						continue;
					}
					// Don't equip a helm if it hides jaw and jaw is locked
					if (ItemInteractions.NO_JAW_HELMS.contains(itemId) && savedSwaps.isKitLocked(KitType.JAW))
					{
						continue;
					}
				}
				else if (slot == KitType.TORSO)
				{
					// Don't equip torso if it hides arms and arms is locked
					if (!ItemInteractions.ARMS_TORSOS.contains(itemId) && savedSwaps.isKitLocked(KitType.ARMS))
					{
						continue;
					}
				}
				candidates.add(new Candidate(itemComposition.getId(), slot));
			}

			if (!candidates.isEmpty() && candidates.size() >= size)
			{
				Candidate best;
				if (size > 1)
				{
					best = candidates.stream()
						.max(Comparator.comparingDouble(c -> colorScorer.score(c.itemId, c.slot)))
						.get();
					colorScorer.addPlayerInfo(best.slot, best.itemId);
				}
				else
				{
					best = candidates.get(0);
				}
				newSwaps.put(best.slot, best.itemId);
				candidates.clear();
			}
		}
		// slots filled with -1 were placeholders that need to be removed
		List<KitType> removes = newSwaps.entrySet().stream()
			.filter(e -> e.getValue() < 0)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		removes.forEach(newSwaps::remove);

		// shuffle colors
		Map<ColorType, Integer> newColors = new HashMap<>();
		List<ColorType> allColorTypes = Arrays.asList(ColorType.values().clone());
		Collections.shuffle(allColorTypes);
		for (ColorType type : allColorTypes)
		{
			if (savedSwaps.isColorLocked(type))
			{
				continue;
			}
			List<Colorable> colorables = Arrays.asList(type.getColorables().clone());
			if (colorables.isEmpty())
			{
				continue;
			}
			Collections.shuffle(colorables);
			int limit;
			switch (intelligence)
			{
				case LOW:
					limit = Math.max(1, colorables.size() / 4);
					break;
				case MODERATE:
					limit = Math.max(1, colorables.size() / 2);
					break;
				case HIGH:
					limit = colorables.size();
					break;
				default:
					limit = 1;
			}
			Colorable best = colorables.stream()
				.limit(limit)
				.max(Comparator.comparingDouble(c -> colorScorer.score(c, type)))
				.orElse(colorables.get(0));
			colorScorer.addPlayerColor(type, best);
			newColors.put(type, best.getColorId(type));
		}

		SwapDiff totalDiff = SwapDiff.blank();

		// convert to equipment ids
		Map<KitType, Integer> newEquipSwaps = newSwaps.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + FashionManager.ITEM_OFFSET));

		// swap items now before moving on to kits
		SwapDiff itemsDiff = CompoundSwap.fromMap(newEquipSwaps, null).stream()
			.map(c -> fashionManager.swap(c, SwapMode.SAVE))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());
		totalDiff = totalDiff.mergeOver(itemsDiff);

		// See if remaining slots can be kit-swapped
		if (fashionManager.getGender() != null && !config.excludeBaseModels())
		{
			Map<KitType, Integer> kitSwaps = Arrays.stream(KitType.values())
				.filter(slot -> !newSwaps.containsKey(slot) && isOpen(slot))
				.map(slot -> {
					List<Kit> kits = KIT_TYPE_TO_KITS.getOrDefault(slot, new ArrayList<>()).stream()
						.filter(k -> k.getKitId(fashionManager.getGender()) != null)
						.collect(Collectors.toList());
					return kits.isEmpty() ? null : kits.get(r.nextInt(kits.size()));
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Kit::getKitType, k -> k.getKitId(fashionManager.getGender())));

			Map<KitType, Integer> kitEquipSwaps = kitSwaps.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() + FashionManager.KIT_OFFSET));

			SwapDiff kitsDiff = CompoundSwap.fromMap(kitEquipSwaps, null)
				.stream()
				.map(c -> fashionManager.swap(c, SwapMode.SAVE))
				.reduce(SwapDiff::mergeOver)
				.orElse(SwapDiff.blank());

			totalDiff = totalDiff.mergeOver(kitsDiff);
		}

		if (!config.excludeNonStandardItems() && !config.excludeMembersItems() && !savedSwaps.isIconLocked())
		{
			List<JawIcon> icons = Arrays.asList(JawIcon.values());
			Collections.shuffle(icons);
			int limit = icons.size();
			if (intelligence == RandomizerIntelligence.NONE)
			{
				limit = 1;
			}
			Map<JawIcon, Double> scores = icons.stream()
				.limit(limit)
				.collect(Collectors.toMap(i -> i, i -> {
					Integer itemId = JawKit.NO_JAW.getIconItemId(i);
					return itemId != null ? colorScorer.score(itemId, null) : 0;
				}));
			// only icon swap if >75% match (if intelligence is > NONE)
			JawIcon icon = scores.entrySet().stream()
				.filter(e -> intelligence == RandomizerIntelligence.NONE || e.getValue() > 0.75)
				.max(Comparator.comparingDouble(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(JawIcon.NOTHING);
			SwapDiff iconDiff = fashionManager.swap(CompoundSwap.fromIcon(icon), SwapMode.PREVIEW, SwapMode.SAVE);
			totalDiff = totalDiff.mergeOver(iconDiff);
		}

		SwapDiff colorsDiff = newColors.entrySet().stream()
			.filter(e -> e.getValue() >= 0)
			.map(e -> fashionManager.swap(e.getKey(), e.getValue(), SwapMode.SAVE))
			.reduce(SwapDiff::mergeOver)
			.orElse(SwapDiff.blank());

		totalDiff = totalDiff.mergeOver(colorsDiff);

		fashionManager.getSwapDiffHistory().appendToUndo(totalDiff);
	}

	/**
	 * @return true if the slot does not have an item (real or virtual) obscuring it (directly or indirectly)
	 */
	public boolean isOpen(KitType slot)
	{
		if (slot == null || savedSwaps.isKitLocked(slot) || savedSwaps.containsItem(slot))
		{
			return false;
		}
		FashionManager fashionManager = fashionManagerProvider.get();
		Supplier<Boolean> fallback = () -> {
			if (fashionManager.equipmentIdInSlot(slot) > 0)
			{
				// equipment id must be a kit since there can't be an item at this point
				return true;
			}
			else
			{
				Integer actualEquipId = fashionManager.inventoryItemId(slot);
				return actualEquipId == null || actualEquipId < FashionManager.ITEM_OFFSET;
			}
		};
		switch (slot)
		{
			case HAIR:
			case JAW:
				int headEquipId = fashionManager.equipmentIdInSlot(KitType.HEAD);
				if (headEquipId > FashionManager.ITEM_OFFSET)
				{
					int headItemId = headEquipId - FashionManager.ITEM_OFFSET;
					if (slot == KitType.HAIR)
					{
						return ItemInteractions.HAIR_HELMS.contains(headItemId);
					}
					return !ItemInteractions.NO_JAW_HELMS.contains(headItemId);
				}
				return fallback.get();
			case ARMS:
				int torsoEquipId = fashionManager.equipmentIdInSlot(KitType.TORSO);
				if (torsoEquipId > FashionManager.ITEM_OFFSET)
				{
					int torsoItemId = torsoEquipId - FashionManager.ITEM_OFFSET;
					return ItemInteractions.ARMS_TORSOS.contains(torsoItemId);
				}
				return fallback.get();
			default:
				return fallback.get();
		}
	}

}
