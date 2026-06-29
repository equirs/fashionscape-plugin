package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class DiffTest
{
	@Test
	void unchangedColorIsEmpty()
	{
		assertTrue(Diff.ofColor(ColorType.HAIR, 5, 5).isEmpty());
		// null -> null is also a no-op
		assertTrue(Diff.ofColor(ColorType.HAIR, null, null).isEmpty());
	}

	@Test
	void colorChange()
	{
		Diff diff = Diff.ofColor(ColorType.HAIR, 1, 2);
		assertFalse(diff.isEmpty());
		assertEquals(1, diff.getOutColors().get(ColorType.HAIR));
		assertEquals(2, diff.getInColors().get(ColorType.HAIR));
	}

	@Test
	void slotChange()
	{
		Diff diff = Diff.ofSlots(
			ImmutableMap.of(KitType.LEGS, TestData.mimeLegs),
			ImmutableMap.of(KitType.LEGS, TestData.flaresLegs));
		assertFalse(diff.isEmpty());
		assertEquals(TestData.mimeLegs, diff.getOutSlots().get(KitType.LEGS));
		assertEquals(TestData.flaresLegs, diff.getInSlots().get(KitType.LEGS));
	}

	@Test
	void iconChange()
	{
		Diff diff = Diff.ofIcon(JawIcon.BA_ATTACKER, JawIcon.BA_HEALER);
		assertFalse(diff.isEmpty());
		assertEquals(JawIcon.BA_ATTACKER, diff.getOutIcon());
		assertEquals(JawIcon.BA_HEALER, diff.getInIcon());
	}

	@Test
	void mergeOfEmptiesIsEmpty()
	{
		assertTrue(Diff.merge(Diff.empty(), Diff.empty()).isEmpty());
	}

	@Test
	void mergeDisjointFields()
	{
		Diff slots = Diff.ofSlots(
			ImmutableMap.of(KitType.LEGS, TestData.mimeLegs),
			ImmutableMap.of(KitType.LEGS, TestData.flaresLegs));
		Diff color = Diff.ofColor(ColorType.HAIR, 1, 2);
		Diff merged = Diff.merge(slots, color);
		assertEquals(TestData.flaresLegs, merged.getInSlots().get(KitType.LEGS));
		assertEquals(2, merged.getInColors().get(ColorType.HAIR));
	}

	@Test
	void mergePrefersFirstDiffOnCollision()
	{
		Diff first = Diff.ofSlots(
			ImmutableMap.of(KitType.LEGS, TestData.mimeLegs),
			ImmutableMap.of(KitType.LEGS, TestData.flaresLegs));
		Diff second = Diff.ofSlots(
			ImmutableMap.of(KitType.LEGS, TestData.corruptedLegs),
			ImmutableMap.of(KitType.LEGS, SlotInfo.nothing(KitType.LEGS)));
		Diff merged = Diff.merge(first, second);
		assertEquals(TestData.mimeLegs, merged.getOutSlots().get(KitType.LEGS));
		assertEquals(TestData.flaresLegs, merged.getInSlots().get(KitType.LEGS));
	}

	@Test
	void iconMergeTakesFirstNonNull()
	{
		Diff withIcon = Diff.ofIcon(JawIcon.BA_ATTACKER, JawIcon.BA_HEALER);
		Diff withoutIcon = Diff.ofColor(ColorType.HAIR, 1, 2);
		assertEquals(JawIcon.BA_ATTACKER, Diff.merge(withIcon, withoutIcon).getOutIcon());
		assertEquals(JawIcon.BA_ATTACKER, Diff.merge(withoutIcon, withIcon).getOutIcon());
	}
}
