package eq.uirs.fashionscape.core.layer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import eq.uirs.fashionscape.core.Diff;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.Exclusions;
import eq.uirs.fashionscape.core.Fallbacks;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.IdleAnimations;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.event.KnownKitChanged;
import eq.uirs.fashionscape.core.model.Items;
import eq.uirs.fashionscape.core.model.Kits;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.MapUtil;
import eq.uirs.fashionscape.data.MiscData;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.remote.RemoteData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Combination of real and plugin-defined (virtual) model information for the player.
 * This information is layered together to compute resulting equipmentIds.
 * Virtual slots should not be in conflict: if an item hides a slot, placing it in layers removes any virtual kit
 * in said slot.
 * Real item and kit data can be contradictory. The generated equipmentIds will take this into account.
 */
@Getter
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Layers
{
	private final IdleAnimations idleAnimations;

	private final ModelInfo realModels = new ModelInfo(ModelType.REAL);
	private final ModelInfo virtualModels = new ModelInfo(ModelType.VIRTUAL);
	// "preview" is a layer on top of (taking precedence over) virtual models
	private final ModelInfo previewModels = new ModelInfo(ModelType.PREVIEW);

	private Integer gender = null;
	private int[] lastEquipmentIds = null;
	private Integer lastRealIdlePoseAnim = null;

	/**
	 * Clears all state relating to player's actual models/colors. Called upon logout.
	 */
	public void resetRealInfo()
	{
		resetModels(realModels);
		gender = null;
		lastEquipmentIds = null;
	}

	/**
	 * Removes preview model layer. Usually, this means the player is no longer hovering over an item.
	 */
	public void resetPreview()
	{
		resetModels(previewModels);
	}

	@VisibleForTesting
	public static void resetModels(ModelInfo info)
	{
		info.getItems().clear();
		info.getKits().clear();
		info.getColors().clear();
		info.putIcon(null);
	}

	// called upon shutting down to reset local player to normal state without clearing plugin state
	public void revertToRealModels(Player player)
	{
		if (player == null)
		{
			return;
		}
		Integer revertedAnimId = computeIdlePoseAnimation(true);
		if (revertedAnimId != null)
		{
			player.setIdlePoseAnimation(revertedAnimId);
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return;
		}
		int[] equipment = computeEquipment(true);
		for (int i = 0; i < equipment.length; i++)
		{
			composition.getEquipmentIds()[i] = equipment[i];
		}
		int[] colors = computeColors(true);
		for (int i = 0; i < colors.length; i++)
		{
			composition.getColors()[i] = colors[i];
		}
		composition.setHash();
	}

	/**
	 * Extracts information about the player after in-game composition changes.
	 * This must be called in between the player changing and refreshing virtual equipment.
	 */
	public void deriveNonEquipment(PlayerComposition composition, int idlePoseAnim)
	{
		if (composition == null)
		{
			return;
		}
		int gender = composition.getGender();
		if (!Objects.equals(gender, this.gender))
		{
			this.gender = gender;
			refreshGenderedKits(gender);
		}
		lastRealIdlePoseAnim = idlePoseAnim;
		deriveColors(composition);
	}

	private void refreshGenderedKits(int gender)
	{
		realModels.getKits().setGender(gender, true);
		virtualModels.getKits().setGender(gender, false);
		previewModels.getKits().setGender(gender, false);
	}

	private void deriveColors(@NotNull PlayerComposition composition)
	{
		int[] colors = composition.getColors();
		ColorType[] colorTypes = ColorType.values();
		Map<ColorType, Integer> typeToColor = IntStream.range(0, colorTypes.length).boxed()
			.collect(Collectors.toMap(i -> colorTypes[i], i -> colors[i]));
		realModels.getColors().putAll(typeToColor);
	}

	public void deriveEquipment(PlayerComposition composition)
	{
		if (composition == null)
		{
			return;
		}
		int[] equipIds = composition.getEquipmentIds();
		// no need to derive again if equipment is unchanged
		if (lastEquipmentIds == equipIds)
		{
			return;
		}
		lastEquipmentIds = equipIds;
		// clear items but retain kits
		realModels.getItems().clear();
		int[] equipmentIds = composition.getEquipmentIds();
		for (KitType slot : KitType.values())
		{
			int equipId = equipmentIds[slot.getIndex()];
			if (equipId >= FashionManager.ITEM_OFFSET)
			{
				int itemId = equipId - FashionManager.ITEM_OFFSET;
				// need to split jaw item into icon+kit and store that. there are no actual items in this slot
				if (slot == KitType.JAW)
				{
					JawKit kit = JawKit.fromEquipmentId(equipId);
					if (kit != null && kit != JawKit.NO_JAW && gender != null)
					{
						realModels.getKits().put(slot, kit.getKitId(gender));
					}
					realModels.putIcon(JawKit.iconFromItemId(itemId));
				}
				else
				{
					realModels.getItems().put(slot, SlotInfo.lookUp(equipId, slot));
				}
			}
			else if (equipId >= FashionManager.KIT_OFFSET)
			{
				int kitId = equipId - FashionManager.KIT_OFFSET;
				realModels.getKits().put(slot, kitId);
				Events.fire(new KnownKitChanged(false, slot));
			}
			// no need to store "nothing" in real info, there's no distinction between that and unset
		}
	}

	/**
	 * Virtually places `info` in `slot`, or unsets if `info` is null.
	 * Returns a diff of incoming+outgoing info.
	 * Jaw items are not supported here; set icons directly and use jaw kits instead.
	 * Modifies preview models if `isPreview`, otherwise modifies virtual models.
	 */
	public Diff set(KitType slot, @Nullable SlotInfo info, boolean isPreview)
	{
		Map<KitType, SlotInfo> outSlots = new HashMap<>();
		Map<KitType, SlotInfo> inSlots = new HashMap<>();
		ModelInfo models = isPreview ? previewModels : virtualModels;
		Items items = models.getItems();
		Kits kits = models.getKits();
		Integer oldKit;
		SlotInfo oldItem;
		if (info == null)
		{
			// the unset operation only requires removing current slot occupant
			oldKit = kits.remove(slot);
			// diff should still make sense: kit+item cannot occupy same slot
			oldItem = items.remove(slot);
		}
		else
		{
			// kit/item will replace current slot occupant
			inSlots.put(slot, info);
			if (info.isKit())
			{
				oldKit = kits.put(slot, info.getKitId());
				oldItem = items.remove(slot);
			}
			else
			{
				oldItem = items.put(slot, info);
				oldKit = kits.remove(slot);
			}
			// items in other slots that this item hides must go
			Set<KitType> removals = info.getHidden().stream()
				.map(items::get)
				.filter(Objects::nonNull)
				.map(SlotInfo::getSlot)
				.collect(Collectors.toSet());
			// kits in other slots that this item hides must go
			info.getHidden().stream()
				.filter(k -> kits.get(k) != null)
				.forEach(removals::add);
			// items in other slots that either hide this slot or share hidden with incoming must go
			items.getAll().values().stream()
				.filter(s -> s.getSlot() != slot &&
					(s.hides(slot) || !Sets.intersection(info.getHidden(), s.getHidden()).isEmpty()))
				.map(SlotInfo::getSlot)
				.forEach(removals::add);
			removals.forEach(s -> {
				Integer outKit = kits.remove(s);
				if (outKit != null)
				{
					outSlots.put(s, SlotInfo.kit(outKit, s));
				}
				SlotInfo outItem = items.remove(s);
				if (outItem != null)
				{
					outSlots.put(s, outItem);
				}
			});
		}
		if (oldKit != null)
		{
			outSlots.put(slot, SlotInfo.kit(oldKit, slot));
		}
		if (oldItem != null)
		{
			outSlots.put(slot, oldItem);
		}
		return Diff.ofSlots(outSlots, inSlots);
	}

	public Diff setIcon(@Nullable JawIcon icon, boolean isPreview)
	{
		ModelInfo models = isPreview ? previewModels : virtualModels;
		JawIcon outIcon = models.putIcon(icon);
		return Diff.ofIcon(outIcon, icon);
	}

	public Diff setColor(ColorType slot, Integer colorId, boolean isPreview)
	{
		ModelInfo models = isPreview ? previewModels : virtualModels;
		Integer outId = models.getColors().put(slot, colorId);
		return Diff.ofColor(slot, outId, colorId);
	}

	/**
	 * Computes array of equipment ids, taking into account virtual and real model info.
	 * In descending order of priority (visibility above others):
	 * Real equipment exceptions (minecart, magic carpet) > preview items > preview kits > virtual items >
	 * virtual kits > real items > real kits.
	 */
	public int[] computeEquipment()
	{
		return computeEquipment(false);
	}

	// if `realOnly`, then preview/virtual models won't be considered
	private int[] computeEquipment(boolean realOnly)
	{
		Map<KitType, Integer> slotToId = new HashMap<>();

		Items realItems = realModels.getItems();
		Kits realKits = realModels.getKits();

		// 1. real temporary equipment models (rare cases)
		// check for minecart
		if (Exclusions.DISABLE_ANIM_BOOTS.contains(realKits.get(KitType.BOOTS)))
		{
			MapUtil.putAllIfAllAbsent(slotToId, ImmutableMap.of(
				KitType.BOOTS, realKits.get(KitType.BOOTS) + FashionManager.KIT_OFFSET,
				KitType.WEAPON, 0,
				KitType.SHIELD, 0
			));
		}
		// check for presence of temporary animation-replacing (and shield-hiding) weapons
		SlotInfo weaponInfo = realItems.get(KitType.WEAPON);
		if (weaponInfo != null && getTempDisabledWeaponIds().contains(weaponInfo.getItemId()))
		{
			MapUtil.putAllIfAllAbsent(slotToId, ImmutableMap.of(
				KitType.WEAPON, weaponInfo.getEquipmentId(),
				KitType.SHIELD, 0
			));
		}
		// check for presence of temporary animation-replacing weapon-shield combos
		SlotInfo shieldInfo = realItems.get(KitType.SHIELD);
		if (weaponInfo != null && getTempDisabledWeaponShieldIds().contains(weaponInfo.getItemId()) ||
			shieldInfo != null && getTempDisabledWeaponShieldIds().contains(shieldInfo.getItemId()))
		{
			MapUtil.putAllIfAllAbsent(slotToId, ImmutableMap.of(
				KitType.WEAPON, weaponInfo != null ? weaponInfo.getEquipmentId() : 0,
				KitType.SHIELD, shieldInfo != null ? shieldInfo.getEquipmentId() : 0
			));
		}
		if (!realOnly)
		{
			// 2. preview items > preview models
			computeFromModels(previewModels, slotToId, false);
			// 3. virtual items > virtual models
			computeFromModels(virtualModels, slotToId, false);
		}
		// 4. real items > real models
		computeFromModels(realModels, slotToId, realOnly);

		// edge case: if jaw has icon, and jaw slot does not contain anything, ensure that icon is shown
		Integer currentJaw = slotToId.get(KitType.JAW);
		JawIcon icon = getDisplayedIcon(realOnly);
		if ((currentJaw == null || currentJaw == 0) && icon != null && icon != JawIcon.NOTHING)
		{
			JawKit kit = JawKit.NO_JAW;
			// if jaw is still null at this point, a fallback will be displayed with the icon
			if (currentJaw == null)
			{
				int kitId = Fallbacks.getKit(KitType.JAW, gender, true);
				JawKit k = JawKit.fromEquipmentId(kitId + FashionManager.KIT_OFFSET);
				if (k != null)
				{
					kit = k;
				}
			}
			Integer itemId = kit.getIconItemId(icon);
			if (itemId != null)
			{
				slotToId.put(KitType.JAW, itemId + FashionManager.ITEM_OFFSET);
			}
		}

		// finally, create and populate the equipment id array
		int[] ids = new int[12];
		for (KitType slot : KitType.values())
		{
			Integer equipId = slotToId.get(slot);
			if (equipId == null)
			{
				// equipId will be 0 if there is no appropriate fallback
				equipId = Fallbacks.getKit(slot, gender, true) + FashionManager.KIT_OFFSET;
			}
			ids[slot.getIndex()] = equipId;
		}
		return ids;
	}

	private void computeFromModels(ModelInfo modelInfo, Map<KitType, Integer> slotToId, boolean realOnly)
	{
		modelInfo.getItems().getAll().forEach((slot, info) ->
			MapUtil.putAllIfAllAbsent(slotToId, info.computeEquipment()));
		modelInfo.getKits().computeEquipment(getDisplayedIcon(realOnly)).forEach(slotToId::putIfAbsent);
	}

	@Nullable
	private JawIcon getDisplayedIcon(boolean realOnly)
	{
		if (!realOnly && previewModels.getIcon() != null)
		{
			return previewModels.getIcon();
		}
		if (!realOnly && virtualModels.getIcon() != null)
		{
			return virtualModels.getIcon();
		}
		return realModels.getIcon();
	}

	private Set<Integer> getTempDisabledWeaponIds()
	{
		MiscData misc = RemoteData.MISC_DATA;
		if (misc == null)
		{
			return new HashSet<>();
		}
		return misc.disableAnimWeapons;
	}

	private Set<Integer> getTempDisabledWeaponShieldIds()
	{
		MiscData misc = RemoteData.MISC_DATA;
		if (misc == null)
		{
			return new HashSet<>();
		}
		return misc.disableAnimWeaponOrShield;
	}

	/**
	 * Determines the player's idle pose anim, based on these criteria, ordered by descending importance:
	 * Real items which temporarily change animations (e.g. magic carpet) > preview items > virtual items > real items.
	 * Returns null if the animation cannot be determined and thus shouldn't be changed.
	 */
	@Nullable
	public Integer computeIdlePoseAnimation()
	{
		return computeIdlePoseAnimation(false);
	}

	// if `realOnly`, then preview/virtual models won't be considered
	@Nullable
	private Integer computeIdlePoseAnimation(boolean realOnly)
	{
		SlotInfo weaponInfo = realModels.getItems().get(KitType.WEAPON);
		if (weaponInfo != null && getTempDisabledWeaponIds().contains(weaponInfo.getItemId()))
		{
			return null;
		}
		if (!realOnly)
		{
			// see if preview/virtual weapon changes anim
			weaponInfo = previewModels.getItems().get(KitType.WEAPON);
			weaponInfo = weaponInfo != null ? weaponInfo : virtualModels.getItems().get(KitType.WEAPON);
			if (weaponInfo != null)
			{
				Integer idle = idleAnimations.get(weaponInfo.getItemId());
				return idle != null ? idle : IdleAnimations.DEFAULT;
			}
		}
		weaponInfo = realModels.getItems().get(KitType.WEAPON);
		if (!realOnly)
		{
			// edge case: if other virtual slots (e.g., shields) hide the real weapon, use the default anim
			if (weaponInfo != null && weaponInfo.getHidden().stream()
				.anyMatch(slot -> virtualModels.contains(slot) || previewModels.contains(slot)))
			{
				return IdleAnimations.DEFAULT;
			}
		}
		// if we know for certain what the real weapon's anim id is, use that
		if (weaponInfo != null)
		{
			Integer idle = idleAnimations.get(weaponInfo.getItemId());
			if (idle != null)
			{
				return idle;
			}
		}
		else
		{
			// there is no weapon
			return IdleAnimations.DEFAULT;
		}
		// otherwise, just fall back on what the last real idle anim was
		return lastRealIdlePoseAnim;
	}

	/**
	 * Determines color ids for the player
	 */
	public int[] computeColors()
	{
		return computeColors(false);
	}

	// if `realOnly`, then preview/virtual colors won't be considered
	private int[] computeColors(boolean realOnly)
	{
		int[] result = new int[5];
		for (ColorType type : ColorType.values())
		{
			Integer value = null;
			if (!realOnly)
			{
				value = previewModels.getColors().get(type);
				value = value == null ? virtualModels.getColors().get(type) : value;
			}
			value = value == null ? realModels.getColors().get(type) : value;
			if (value != null)
			{
				result[type.ordinal()] = value;
			}
		}
		return result;
	}

	/**
	 * Populates virtual models with data saved in plugin config. Does not modify diff History.
	 */
	public void restore(Map<KitType, SlotInfo> equipInfo, Map<ColorType, Integer> colorIds, @Nullable JawIcon icon)
	{
		resetModels(virtualModels);
		Items items = virtualModels.getItems();
		Kits kits = virtualModels.getKits();
		equipInfo.forEach((slot, info) -> {
			if (info.isKit())
			{
				kits.put(slot, info.getKitId());
			}
			else
			{
				items.put(slot, info);
			}
		});
		virtualModels.putIcon(icon);
		virtualModels.getColors().putAll(colorIds);
	}

	/**
	 * Populate real kits with data saved in RS profile config.
	 */
	public void restoreFromRSProfile(HashMap<KitType, Integer> realKits)
	{
		Kits kits = realModels.getKits();
		realKits.forEach(kits::put);
	}
}
