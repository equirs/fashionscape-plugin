package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.BaseTest;
import eq.uirs.fashionscape.data.kit.BootsKit;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.data.kit.JawKit;
import eq.uirs.fashionscape.data.kit.TorsoKit;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Value;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayersComputeTest extends BaseTest
{
	private static int fallback(KitType slot)
	{
		return Fallbacks.getKit(slot, 0, false) + FashionManager.KIT_OFFSET;
	}

	@Value
	public static class Case
	{
		// setup
		Map<KitType, SlotInfo> realItems;
		Map<KitType, Integer> realKits;
		JawIcon realIcon;
		Map<KitType, SlotInfo> virtualItems;
		Map<KitType, Integer> virtualKits;
		JawIcon virtualIcon;
		// output
		int[] equipIds;
	}

	public static Stream<Case> provideCases()
	{
		return Stream.of(
			// everything empty -> fallbacks
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(),
				JawIcon.NOTHING,
				ImmutableMap.of(),
				ImmutableMap.of(),
				JawIcon.NOTHING,
				new int[]{
					0,
					0,
					0,
					0,
					fallback(KitType.TORSO),
					0,
					fallback(KitType.ARMS),
					fallback(KitType.LEGS),
					fallback(KitType.HAIR),
					fallback(KitType.HANDS),
					fallback(KitType.BOOTS),
					fallback(KitType.JAW)
				}
			),
			new Case(
				// cape unobstructed, shield obstructed by virtual 2h, boots obstructed by virtual kit
				ImmutableMap.of(
					KitType.CAPE, TestData.blueCape,
					KitType.SHIELD, TestData.gildedKiteshield,
					KitType.BOOTS, TestData.pinkBoots
				),
				// torso obstructed by virtual, legs unobstructed
				ImmutableMap.of(
					KitType.TORSO, TorsoKit.SWEATER.getMascKitId(),
					KitType.LEGS, TestData.flaresLegs.getKitId()
				),
				JawIcon.NOTHING,
				ImmutableMap.of(KitType.WEAPON, TestData.white2hSword),
				// legs are unset but present in real kits
				new ImmutableMap.Builder<KitType, Integer>()
					.put(KitType.TORSO, TestData.plainTorso.getKitId())
					.put(KitType.ARMS, TestData.thinStripeArms.getKitId())
					.put(KitType.HAIR, TestData.curlsHair.getKitId())
					.put(KitType.HANDS, TestData.bracersHands.getKitId())
					.put(KitType.BOOTS, TestData.smallBoots.getKitId())
					.put(KitType.JAW, TestData.daliJaw.getKitId())
					.build(),
				JawIcon.NOTHING,
				TestData.comp1
			),
			// virtual items showing real kits (i.e., real items hide but virtual items don't)
			new Case(
				ImmutableMap.of(
					KitType.HEAD, TestData.ironFullHelm,
					KitType.WEAPON, TestData.blackMace,
					KitType.LEGS, TestData.mimeLegs,
					KitType.HANDS, TestData.leatherGloves
				),
				// all kits obstructed
				ImmutableMap.of(
					KitType.JAW, TestData.daliJaw.getKitId(),
					KitType.LEGS, TestData.flaresLegs.getKitId(),
					KitType.TORSO, TestData.plainTorso.getKitId(),
					KitType.HAIR, TestData.curlsHair.getKitId()
				),
				// icon obstructed
				JawIcon.BA_HEALER,
				ImmutableMap.of(
					KitType.AMULET, TestData.camulet,
					KitType.TORSO, TestData.bronzePlatebody,
					KitType.SHIELD, TestData.gildedKiteshield,
					KitType.BOOTS, TestData.pinkBoots
				),
				ImmutableMap.of(),
				JawIcon.BA_DEFENDER,
				TestData.comp2
			),
			new Case(
				// weapon / shield obstructed by minecart
				ImmutableMap.of(KitType.WEAPON, TestData.white2hSword),
				ImmutableMap.of(KitType.BOOTS, BootsKit.MINECART.getMascKitId()),
				JawIcon.NOTHING,
				// weapon / shield obstructed by minecart, cape unobstructed
				ImmutableMap.of(
					KitType.WEAPON, TestData.blackMace,
					KitType.SHIELD, TestData.gildedKiteshield,
					KitType.CAPE, TestData.hoodedCloak
				),
				// boots obstructed by minecart, legs unobstructed
				ImmutableMap.of(
					KitType.BOOTS, TestData.smallBoots.getKitId(),
					KitType.LEGS, TestData.flaresLegs.getKitId()
				),
				JawIcon.NOTHING,
				new int[]{
					0,
					TestData.hoodedCloak.getEquipmentId(),
					0,
					0,
					fallback(KitType.TORSO),
					0,
					fallback(KitType.ARMS),
					TestData.flaresLegs.getEquipmentId(),
					0,
					fallback(KitType.HANDS),
					BootsKit.MINECART.getMascKitId() + FashionManager.KIT_OFFSET,
					// add icon to whatever fallback jaw is
					fallback(KitType.JAW)
				}
			),
			new Case(
				ImmutableMap.of(
					KitType.WEAPON, SlotInfo.item(ItemID.MAGIC_CARPET, KitType.WEAPON)
				),
				ImmutableMap.of(),
				JawIcon.NOTHING,
				// weapon / shield obstructed by magic carpet
				ImmutableMap.of(KitType.WEAPON, TestData.white2hSword),
				ImmutableMap.of(),
				JawIcon.NOTHING,
				new int[]{
					0,
					0,
					0,
					ItemID.MAGIC_CARPET + FashionManager.ITEM_OFFSET,
					fallback(KitType.TORSO),
					0,
					fallback(KitType.ARMS),
					fallback(KitType.LEGS),
					fallback(KitType.HAIR),
					fallback(KitType.HANDS),
					fallback(KitType.BOOTS),
					fallback(KitType.JAW)
				}
			),
			// all fallbacks but with jaw icon (covers edge case)
			new Case(
				ImmutableMap.of(),
				ImmutableMap.of(),
				JawIcon.NOTHING,
				ImmutableMap.of(),
				ImmutableMap.of(),
				JawIcon.SW_BLUE,
				new int[]{
					0,
					0,
					0,
					0,
					fallback(KitType.TORSO),
					0,
					fallback(KitType.ARMS),
					fallback(KitType.LEGS),
					fallback(KitType.HAIR),
					fallback(KitType.HANDS),
					fallback(KitType.BOOTS),
					// add icon to whatever fallback jaw is
					JawKit.getEquipmentId(fallback(KitType.JAW) - FashionManager.KIT_OFFSET, JawIcon.SW_BLUE)
				}
			)
		);
	}

	@ParameterizedTest
	@MethodSource("provideCases")
	void computes(Case c)
	{
		PlayerComposition composition = mock(PlayerComposition.class);
		when(composition.getGender()).thenReturn(0);
		when(composition.getColors()).thenReturn(new int[5]);
		layers.deriveNonEquipment(composition, 0);
		layers.deriveEquipment(composition);

		setKitsOnModels(c.realKits, layers.getRealModels());
		layers.getRealModels().getItems().putAll(c.realItems);
		layers.getRealModels().putIcon(c.realIcon);

		setKitsOnModels(c.virtualKits, layers.getVirtualModels());
		layers.getVirtualModels().getItems().putAll(c.virtualItems);
		layers.getVirtualModels().putIcon(c.virtualIcon);

		assertArrayEquals(c.equipIds, layers.computeEquipment());
		resetData();
	}
}
