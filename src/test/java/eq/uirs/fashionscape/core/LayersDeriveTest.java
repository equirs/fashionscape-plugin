package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.BaseTest;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.data.color.BootsColor;
import eq.uirs.fashionscape.data.color.ClothingColor;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.HairColor;
import eq.uirs.fashionscape.data.color.SkinColor;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayersDeriveTest extends BaseTest
{
	@Value
	public static class Case
	{
		// setup
		int[] equipIds;
		// output
		Map<KitType, SlotInfo> items;
		Map<KitType, Integer> kits;
		JawIcon icon;
	}

	private PlayerComposition composition;

	private final int hairColor = HairColor.AQUA.getColorId();
	private final int torsoColor = ClothingColor.BLACK.getColorId(ColorType.TORSO);
	private final int legsColor = ClothingColor.WHITE.getColorId(ColorType.LEGS);
	private final int bootsColor = BootsColor.BROWN.getColorId();
	private final int skinColor = SkinColor.CYAN.getColorId();
	private final int[] colors = {hairColor, torsoColor, legsColor, bootsColor, skinColor};

	public static Stream<Case> provideCases()
	{
		return Stream.of(
			new Case(
				TestData.comp1,
				ImmutableMap.of(
					KitType.CAPE, TestData.blueCape,
					KitType.WEAPON, TestData.white2hSword
				),
				new ImmutableMap.Builder<KitType, Integer>()
					.put(KitType.TORSO, TestData.plainTorso.getKitId())
					.put(KitType.ARMS, TestData.thinStripeArms.getKitId())
					.put(KitType.LEGS, TestData.flaresLegs.getKitId())
					.put(KitType.HAIR, TestData.curlsHair.getKitId())
					.put(KitType.HANDS, TestData.bracersHands.getKitId())
					.put(KitType.BOOTS, TestData.smallBoots.getKitId())
					.put(KitType.JAW, TestData.daliJaw.getKitId())
					.build(),
				null),
			new Case(
				TestData.comp2,
				new ImmutableMap.Builder<KitType, SlotInfo>()
					.put(KitType.HEAD, TestData.ironFullHelm)
					.put(KitType.AMULET, TestData.camulet)
					.put(KitType.WEAPON, TestData.blackMace)
					.put(KitType.TORSO, TestData.bronzePlatebody)
					.put(KitType.SHIELD, TestData.gildedKiteshield)
					.put(KitType.LEGS, TestData.mimeLegs)
					.put(KitType.HANDS, TestData.leatherGloves)
					.put(KitType.BOOTS, TestData.pinkBoots)
					.build(),
				ImmutableMap.of(),
				JawIcon.BA_DEFENDER)
		);
	}

	@ParameterizedTest
	@MethodSource("provideCases")
	void testEquipment(Case c)
	{
		mockComposition(c.equipIds);
		layers.deriveNonEquipment(composition, 0);
		layers.deriveEquipment(composition);
		ModelInfo realModels = layers.getRealModels();
		assertEquals(c.items, realModels.getItems().getAll());
		assertEquals(c.kits, realModels.getKits().getAll());
		assertEquals(c.icon, realModels.getIcon());
		resetData();
	}

	@Test
	void testColors()
	{
		// equipment ids are irrelevant
		mockComposition(new int[12]);
		layers.deriveNonEquipment(composition, 0);
		Map<ColorType, Integer> expected = Arrays.stream(ColorType.values())
			.collect(Collectors.toMap(c -> c, c -> colors[c.ordinal()]));
		assertEquals(expected, layers.getRealModels().getColors().getAll());
	}

	private void mockComposition(int[] equipmentIds)
	{
		composition = mock(PlayerComposition.class);
		// behavior is equivalent for masc/fem
		when(composition.getGender()).thenReturn(0);
		when(composition.getColors()).thenReturn(colors);
		when(composition.getEquipmentIds()).thenReturn(equipmentIds);
		when(composition.getEquipmentId(any()))
			.thenAnswer(input -> {
					KitType slot = input.getArgument(0);
					return equipmentIds[slot.getIndex()];
				}
			);
	}
}
