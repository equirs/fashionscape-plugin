package eq.uirs.fashionscape.core.model;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.KitChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.Kit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.Nullable;

/**
 * Represents non-item model information about the player's composition.
 * Values are kit ids. Zero is not allowed (Items class handles "nothing").
 * These may remain in place even if any equipped items obscure the models, since items take priority.
 */
@Data
@Setter(AccessLevel.PRIVATE)
public class Kits
{
	private final ModelType modelType;
	// values are kit ids
	private final Map<KitType, Integer> value = new HashMap<>();

	public ImmutableMap<KitType, Integer> getAll()
	{
		return ImmutableMap.copyOf(value);
	}

	public Integer get(KitType slot)
	{
		return value.get(slot);
	}

	public boolean containsKey(KitType slot)
	{
		return get(slot) != null;
	}

	public Integer remove(KitType slot)
	{
		return put(slot, null);
	}

	public Integer put(KitType slot, Integer kitId)
	{
		Integer oldId = kitId == null ? value.remove(slot) : value.put(slot, kitId);
		Events.fire(new KitChanged(slot, modelType, kitId));
		return oldId;
	}

	public void clear()
	{
		Set<KitType> removes = new HashSet<>(value.keySet());
		removes.forEach(this::remove);
	}

	/**
	 * Replaces any stored kit ids with new ones for the given gender.
	 * If `destructive`, the wrongly-gendered kits will be removed.
	 * Otherwise, will attempt to replace the kit id with an analog kit id.
	 */
	public void setGender(int gender, boolean destructive)
	{
		for (KitType slot : KitType.values())
		{
			replaceGenderedKit(slot, gender, destructive);
		}
	}

	private void replaceGenderedKit(KitType slot, int newGender, boolean destructive)
	{
		Integer kitId = get(slot);
		if (kitId != null)
		{
			// check whether the kit is already correct for this gender
			Kit kit = KitUtil.KIT_ID_TO_KIT.get(kitId);
			if (kit != null && Objects.equals(kit.getKitId(newGender), newGender))
			{
				return;
			}
		}
		else
		{
			// nothing in the slot -> no need to replace
			return;
		}
		if (destructive)
		{
			put(slot, null);
		}
		else
		{
			Kit kit = KitUtil.getWithAnalog(kitId, newGender);
			Integer newKitId = kit != null ? kit.getKitId(newGender) : null;
			put(slot, newKitId);
		}
	}

	/**
	 * Returns map of equipment ids for each slot. The jaw slot will combine with the icon argument.
	 */
	public Map<KitType, Integer> computeEquipment(@Nullable JawIcon icon)
	{
		return value.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> {
				KitType slot = e.getKey();
				Integer kitId = e.getValue();
				return slot == KitType.JAW ? JawKit.getEquipmentId(kitId, icon) : kitId + FashionManager.KIT_OFFSET;
			}));
	}
}
