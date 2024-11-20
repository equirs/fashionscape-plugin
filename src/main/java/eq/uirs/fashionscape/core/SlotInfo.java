package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.data.kit.Kit;
import eq.uirs.fashionscape.remote.RemoteData;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import net.runelite.api.kit.KitType;

/**
 * Collected data about the occupant of a single slot.
 * An instance may be an item, a kit, or nothing.
 */
@Value
public class SlotInfo implements Serializable
{
	int equipmentId;
	@NonNull
	KitType slot;
	// for items: hidden info is determined from wearpos values from cache
	@NonNull
	Set<KitType> hidden;

	public static SlotInfo item(int itemId, KitType slot, KitType... hidden)
	{
		return new SlotInfo(itemId + FashionManager.ITEM_OFFSET, slot,
			Arrays.stream(hidden).collect(Collectors.toSet()));
	}

	public static SlotInfo kit(Kit k, int gender)
	{
		return kit(k.getKitId(gender), k.getKitType());
	}

	public static SlotInfo kit(int kitId, KitType slot)
	{
		return new SlotInfo(kitId + FashionManager.KIT_OFFSET, slot, new HashSet<>());
	}

	public static SlotInfo nothing(KitType slot)
	{
		return new SlotInfo(0, slot, new HashSet<>());
	}

	/**
	 * Most general way to create new SlotInfo.
	 * Uses ExternalData to look up hidden slots in the case of items.
	 * Ensure that jaw slot items are converted to kit ids before calling, and that icon is handled separately.
	 */
	public static SlotInfo lookUp(int equipId, KitType slot)
	{
		if (equipId <= FashionManager.KIT_OFFSET)
		{
			return nothing(slot);
		}
		else if (equipId <= FashionManager.ITEM_OFFSET)
		{
			return kit(equipId - FashionManager.KIT_OFFSET, slot);
		}
		else
		{
			int itemId = equipId - FashionManager.ITEM_OFFSET;
			Map<Integer, ItemSlotInfo> idToSlotInfo = RemoteData.ITEM_ID_TO_INFO;
			ItemSlotInfo info = idToSlotInfo.get(itemId);
			if (info == null || info.hidden0 == null && info.hidden1 == null)
			{
				return item(itemId, slot);
			}
			KitType[] types = KitType.values();
			Set<KitType> hidden = new HashSet<>();
			if (info.hidden0 != null)
			{
				hidden.add(types[info.hidden0]);
			}
			if (info.hidden1 != null)
			{
				hidden.add(types[info.hidden1]);
			}
			return new SlotInfo(itemId + FashionManager.ITEM_OFFSET, slot, hidden);
		}
	}

	public int getItemId()
	{
		return equipmentId - FashionManager.ITEM_OFFSET;
	}

	public int getKitId()
	{
		return equipmentId - FashionManager.KIT_OFFSET;
	}

	public boolean isItem()
	{
		return equipmentId >= FashionManager.ITEM_OFFSET;
	}

	public boolean isKit()
	{
		return equipmentId >= FashionManager.KIT_OFFSET && equipmentId < FashionManager.ITEM_OFFSET;
	}

	public boolean isNothing()
	{
		return equipmentId == 0;
	}

	// whether the given slot is hidden while this model is active
	public boolean hides(KitType kit)
	{
		return hidden.contains(kit);
	}

	public Map<KitType, Integer> computeEquipment()
	{
		Map<KitType, Integer> result = new HashMap<>();
		result.put(slot, equipmentId);
		hidden.forEach(s -> result.put(s, 0));
		return result;
	}
}
