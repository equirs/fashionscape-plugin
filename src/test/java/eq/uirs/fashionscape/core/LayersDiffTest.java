package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.BaseTest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Value;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LayersDiffTest extends BaseTest
{
	@Value
	public static class Case
	{
		// setup
		Map<KitType, Integer> kits;
		Map<KitType, SlotInfo> items;
		// input
		KitType slot;
		SlotInfo info;
		// output
		Map<KitType, SlotInfo> outSlots;
	}

	public static Stream<Case> provideCases()
	{
		return Stream.of(
			// no change
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(),
				KitType.AMULET,
				null,
				ImmutableMap.of()),
			// simple amulet add
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(),
				KitType.AMULET,
				TestData.camulet,
				ImmutableMap.of()),
			// simple helm replace
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(KitType.HEAD, TestData.faceMask),
				KitType.HEAD,
				TestData.redPartyHat,
				ImmutableMap.of(KitType.HEAD, TestData.faceMask)),
			// boot item replaces kit
			new Case(
				ImmutableMap.of(KitType.BOOTS, TestData.smallBoots.getKitId()),
				ImmutableMap.of(),
				KitType.BOOTS,
				TestData.pinkBoots,
				ImmutableMap.of(KitType.BOOTS, TestData.smallBoots)),
			// torso kit replaces item (legs are unaffected)
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(KitType.TORSO, TestData.bronzePlatebody,
					KitType.LEGS, TestData.mimeLegs),
				KitType.TORSO,
				TestData.plainTorso,
				ImmutableMap.of(KitType.TORSO, TestData.bronzePlatebody)),
			// torso item replaces torso+arms kits
			new Case(
				ImmutableMap.of(
					KitType.TORSO, TestData.plainTorso.getKitId(),
					KitType.ARMS, TestData.thinStripeArms.getKitId()
				),
				ImmutableMap.of(),
				KitType.TORSO,
				TestData.bronzePlatebody,
				ImmutableMap.of(KitType.TORSO, TestData.plainTorso,
					KitType.ARMS, TestData.thinStripeArms)),
			// shield replaces 2h weapon
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(KitType.WEAPON, TestData.white2hSword),
				KitType.SHIELD,
				TestData.gildedKiteshield,
				ImmutableMap.of(KitType.WEAPON, TestData.white2hSword)),
			// weapon replaces shield and hands kit
			new Case(
				ImmutableMap.of(KitType.HANDS, TestData.bracersHands.getKitId()),
				ImmutableMap.of(KitType.SHIELD, TestData.gildedKiteshield),
				KitType.WEAPON,
				TestData.beachBoxingGloves,
				ImmutableMap.of(KitType.SHIELD, TestData.gildedKiteshield,
					KitType.HANDS, TestData.bracersHands)),
			// cloak replaces hair kit
			new Case(
				ImmutableMap.of(KitType.HAIR, TestData.curlsHair.getKitId()),
				ImmutableMap.of(),
				KitType.CAPE,
				TestData.hoodedCloak,
				ImmutableMap.of(KitType.HAIR, TestData.curlsHair)),
			// torso item replaces weapon (since both hide hands) and arms kit
			new Case(
				ImmutableMap.of(KitType.ARMS, TestData.thinStripeArms.getKitId()),
				ImmutableMap.of(KitType.WEAPON, TestData.beachBoxingGloves),
				KitType.TORSO,
				TestData.plagueJacket,
				ImmutableMap.of(KitType.WEAPON, TestData.beachBoxingGloves,
					KitType.ARMS, TestData.thinStripeArms))
		);
	}

	@ParameterizedTest
	@MethodSource("provideCases")
	void diffs(Case c)
	{
		setKitsOnModels(c.kits, layers.getVirtualModels());
		layers.getVirtualModels().getItems().putAll(c.items);
		Diff d = layers.set(c.slot, c.info, false);
		Map<KitType, SlotInfo> inSlots = new HashMap<>();
		if (c.info != null)
		{
			inSlots.put(c.slot, c.info);
		}
		assertEquals(Diff.ofSlots(c.outSlots, inSlots), d);
		resetData();
	}
}
