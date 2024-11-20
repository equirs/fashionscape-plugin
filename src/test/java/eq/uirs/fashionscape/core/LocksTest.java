package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.testing.fieldbinder.Bind;
import eq.uirs.fashionscape.BaseTest;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.data.color.ColorType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Value;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class LocksTest extends BaseTest
{
	@Value
	public static class AllowCase
	{
		// setup
		Map<KitType, LockStatus> locks;
		Set<SlotInfo> items;
		// input
		KitType slot;
		SlotInfo info;
	}

	@Value
	public static class DisallowCase
	{
		// setup
		Map<KitType, LockStatus> locks;
		Set<SlotInfo> items;
		// input
		KitType slot;
		SlotInfo info;
		// output
		Set<KitType> conflicts;
	}

	@Bind
	Locks locks = Mockito.spy(new Locks(layers));

	public static Stream<AllowCase> provideAllows()
	{
		return Stream.of(
			// if there are no locks, any modification is allowed
			new AllowCase(new HashMap<>(), new HashSet<>(), KitType.AMULET, TestData.camulet),
			// if only item is locked, changing to kit is allowed
			new AllowCase(
				ImmutableMap.of(KitType.LEGS, LockStatus.ITEM),
				new HashSet<>(),
				KitType.LEGS,
				TestData.flaresLegs),
			// if only item is locked, unsetting a kit is allowed
			new AllowCase(
				ImmutableMap.of(KitType.LEGS, LockStatus.ITEM),
				ImmutableSet.of(TestData.flaresLegs),
				KitType.LEGS,
				null),
			// neither helm changes jaw
			new AllowCase(
				ImmutableMap.of(KitType.JAW, LockStatus.ALL),
				ImmutableSet.of(TestData.redPartyHat, TestData.daliJaw),
				KitType.HEAD,
				TestData.runeMedHelm),
			// helm affects neither hair nor jaw
			new AllowCase(
				ImmutableMap.of(KitType.JAW, LockStatus.ALL, KitType.HAIR, LockStatus.ALL),
				ImmutableSet.of(TestData.redPartyHat),
				KitType.HEAD,
				null),
			// changing to 1h weapon doesn't affect shield
			new AllowCase(
				ImmutableMap.of(KitType.SHIELD, LockStatus.ALL),
				new HashSet<>(),
				KitType.WEAPON,
				TestData.blackMace),
			// even though hair is locked, both helms hide hair
			new AllowCase(
				ImmutableMap.of(KitType.HAIR, LockStatus.ALL),
				ImmutableSet.of(TestData.runeMedHelm),
				KitType.HEAD,
				TestData.ironFullHelm),
			// plague jacket and boxing gloves both hide hands from different slots
			new AllowCase(
				ImmutableMap.of(KitType.HANDS, LockStatus.ALL),
				ImmutableSet.of(TestData.plagueJacket),
				KitType.WEAPON,
				TestData.beachBoxingGloves)
		);
	}

	@ParameterizedTest
	@MethodSource("provideAllows")
	void allowed(AllowCase c)
	{
		c.locks.forEach((key, value) -> locks.set(key, value));
		c.items.forEach(i -> layers.getVirtualModels().getItems().put(i.getSlot(), i));
		assertTrue(locks.isAllowed(c.slot, c.info));
		resetData();
	}

	public static Stream<DisallowCase> provideDisallows()
	{
		return Stream.of(
			// can't place item in a locked slot
			new DisallowCase(
				ImmutableMap.of(KitType.AMULET, LockStatus.ITEM),
				new HashSet<>(),
				KitType.AMULET,
				TestData.camulet,
				ImmutableSet.of(KitType.AMULET)),
			// if item is locked, unsetting it isn't allowed
			new DisallowCase(
				ImmutableMap.of(KitType.CAPE, LockStatus.ITEM),
				ImmutableSet.of(TestData.blueCape),
				KitType.CAPE,
				null,
				ImmutableSet.of(KitType.CAPE)),
			// if item is locked, changing item to kit isn't allowed
			new DisallowCase(
				ImmutableMap.of(KitType.LEGS, LockStatus.ITEM),
				ImmutableSet.of(TestData.mimeLegs),
				KitType.LEGS,
				TestData.flaresLegs,
				ImmutableSet.of(KitType.LEGS)),
			// changing torsos would change the locked arms slot
			new DisallowCase(
				ImmutableMap.of(KitType.ARMS, LockStatus.ALL),
				ImmutableSet.of(TestData.bronzePlatebody),
				KitType.TORSO,
				TestData.studdedBody,
				ImmutableSet.of(KitType.ARMS)),
			// removing torso would change both locked slots
			new DisallowCase(
				ImmutableMap.of(KitType.ARMS, LockStatus.ALL, KitType.TORSO, LockStatus.ALL),
				ImmutableSet.of(TestData.bronzePlatebody),
				KitType.TORSO,
				null,
				ImmutableSet.of(KitType.ARMS, KitType.TORSO)),
			// gauntlet legs would hide locked boots slot
			new DisallowCase(
				ImmutableMap.of(KitType.BOOTS, LockStatus.ITEM),
				ImmutableSet.of(TestData.pinkBoots),
				KitType.LEGS,
				TestData.corruptedLegs,
				ImmutableSet.of(KitType.BOOTS)),
			// changing shields would change the locked weapon slot
			new DisallowCase(
				ImmutableMap.of(KitType.WEAPON, LockStatus.ITEM),
				ImmutableSet.of(TestData.white2hSword),
				KitType.SHIELD,
				TestData.gildedKiteshield,
				ImmutableSet.of(KitType.WEAPON)),
			// changing jaw would change the locked hair slot, due to helm
			new DisallowCase(
				ImmutableMap.of(KitType.HAIR, LockStatus.ALL),
				ImmutableSet.of(TestData.ironFullHelm),
				KitType.JAW,
				TestData.daliJaw,
				ImmutableSet.of(KitType.HAIR)),
			// changing hair would change the locked cape slot
			new DisallowCase(
				ImmutableMap.of(KitType.CAPE, LockStatus.ALL),
				ImmutableSet.of(TestData.hoodedCloak),
				KitType.HAIR,
				TestData.curlsHair,
				ImmutableSet.of(KitType.CAPE)),
			// changing hair would change capes, which would change the locked head slot
			new DisallowCase(
				ImmutableMap.of(KitType.HEAD, LockStatus.ALL),
				ImmutableSet.of(TestData.hoodedCloak),
				KitType.HAIR,
				TestData.curlsHair,
				ImmutableSet.of(KitType.HEAD)),
			// head slot would technically be unchanged by this, but the cloak hides it and is thus tied to it
			new DisallowCase(
				ImmutableMap.of(KitType.HEAD, LockStatus.ALL),
				ImmutableSet.of(TestData.hoodedCloak),
				KitType.CAPE,
				null,
				ImmutableSet.of(KitType.HEAD)),
			// changing gloves would remove torso item and thereby change arms
			new DisallowCase(
				ImmutableMap.of(KitType.ARMS, LockStatus.ALL),
				ImmutableSet.of(TestData.plagueJacket),
				KitType.HANDS,
				TestData.leatherGloves,
				ImmutableSet.of(KitType.ARMS))
		);
	}

	@ParameterizedTest
	@MethodSource("provideDisallows")
	void disallowed(DisallowCase c)
	{
		c.locks.forEach((key, value) -> locks.set(key, value));
		c.items.forEach(i -> layers.getVirtualModels().getItems().put(i.getSlot(), i));
		assertEquals(c.conflicts, locks.conflictingSlots(c.slot, c.info));
		resetData();
	}

	@Override
	protected void resetData()
	{
		for (KitType k : KitType.values())
		{
			locks.set(k, null);
		}
		for (ColorType c : ColorType.values())
		{
			locks.set(c, false);
		}
		locks.setIcon(false);
		super.resetData();
	}
}
