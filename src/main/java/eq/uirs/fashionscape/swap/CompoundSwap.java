package eq.uirs.fashionscape.swap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.kit.KitType;

/**
 * Several slots are tightly coupled in their interactions. This groups them together so
 * that they're submitted for swapping at the same time.
 */
class CompoundSwap
{
	@Getter
	@NonNull
	private final Type type;

	@Getter
	@NonNull
	private final Map<KitType, Integer> equipmentIds;

	public static List<CompoundSwap> fromMap(Map<KitType, Integer> equipmentIds)
	{
		List<CompoundSwap> result = new ArrayList<>();
		Map<Type, Map<KitType, Integer>> grouping = equipmentIds.entrySet().stream()
			.collect(Collectors.groupingBy(
				e -> Type.fromSlot(e.getKey()),
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		Map<KitType, Integer> heads = grouping.get(Type.HEAD);
		if (heads != null && !heads.isEmpty())
		{
			result.add(head(heads.get(KitType.HEAD), heads.get(KitType.HAIR), heads.get(KitType.JAW)));
		}
		Map<KitType, Integer> torsos = grouping.get(Type.TORSO);
		if (torsos != null && !torsos.isEmpty())
		{
			result.add(torso(torsos.get(KitType.TORSO), torsos.get(KitType.ARMS)));
		}
		Map<KitType, Integer> weapons = grouping.get(Type.WEAPONS);
		if (weapons != null && !weapons.isEmpty())
		{
			result.add(weapons(weapons.get(KitType.WEAPON), weapons.get(KitType.SHIELD)));
		}
		Map<KitType, Integer> singles = grouping.get(Type.SINGLE);
		if (singles != null && !singles.isEmpty())
		{
			singles.forEach((slot, equipId) -> result.add(single(slot, equipId)));
		}
		return result;
	}

	enum Type
	{
		// hair jaw head
		HEAD,
		// torso arms
		TORSO,
		// weapon shield
		WEAPONS,
		// any one slot
		SINGLE;

		private static Type fromSlot(KitType slot)
		{
			switch (slot)
			{
				case HEAD:
				case HAIR:
				case JAW:
					return HEAD;
				case TORSO:
				case ARMS:
					return TORSO;
				case WEAPON:
				case SHIELD:
					return WEAPONS;
				default:
					return SINGLE;
			}
		}
	}

	static CompoundSwap single(KitType slot, Integer equipmentId)
	{
		switch (slot)
		{
			case HEAD:
				return head(equipmentId, null, null);
			case HAIR:
				return head(null, equipmentId, null);
			case JAW:
				return head(null, null, equipmentId);
			case TORSO:
				return torso(equipmentId, null);
			case ARMS:
				return torso(null, equipmentId);
			case WEAPON:
				return weapons(equipmentId, null);
			case SHIELD:
				return weapons(null, equipmentId);
		}
		Map<KitType, Integer> map = new HashMap<>();
		map.put(slot, equipmentId);
		return new CompoundSwap(Type.SINGLE, map);
	}

	static CompoundSwap head(Integer headEquipId, Integer hairEquipId, Integer jawEquipId)
	{
		Map<KitType, Integer> map = new HashMap<>();
		map.put(KitType.HEAD, headEquipId);
		map.put(KitType.HAIR, hairEquipId);
		map.put(KitType.JAW, jawEquipId);
		return new CompoundSwap(Type.HEAD, map);
	}

	static CompoundSwap torso(Integer torsoEquipId, Integer armsEquipId)
	{
		Map<KitType, Integer> map = new HashMap<>();
		map.put(KitType.TORSO, torsoEquipId);
		map.put(KitType.ARMS, armsEquipId);
		return new CompoundSwap(Type.TORSO, map);
	}

	static CompoundSwap weapons(Integer weaponEquipId, Integer shieldEquipId)
	{
		Map<KitType, Integer> map = new HashMap<>();
		map.put(KitType.WEAPON, weaponEquipId);
		map.put(KitType.SHIELD, shieldEquipId);
		return new CompoundSwap(Type.WEAPONS, map);
	}

	private CompoundSwap(Type type, Map<KitType, Integer> equipmentIds)
	{
		this.type = type;
		this.equipmentIds = equipmentIds;
	}

	/**
	 * @return the sole equipment id for this swap request. this will only make sense for SINGLE type swaps.
	 */
	Integer getEquipmentId()
	{
		return equipmentIds.values().stream().findAny().orElse(null);
	}

	/**
	 * @return the sole kit slot for this swap request. this will only make sense for SINGLE type swaps.
	 */
	KitType getKitType()
	{
		return equipmentIds.keySet().stream().findAny().orElse(null);
	}
}
