package eq.uirs.fashionscape.core;

import com.google.inject.Inject;
import eq.uirs.fashionscape.base.BaseLayersTest;
import eq.uirs.fashionscape.data.anim.AnimationData;
import eq.uirs.fashionscape.remote.RemoteData;
import java.util.Collections;
import net.runelite.api.ItemComposition;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayersIdleAnimTest extends BaseLayersTest
{
	private static final int TWO_HANDED_ANIM = 2561;

	private final SlotInfo shield = SlotInfo.item(ItemID.RUNE_KITESHIELD, KitType.SHIELD);

	@Inject
	private IdleAnimations idleAnimations;

	@BeforeEach
	void setUpPlayer()
	{
		PlayerComposition composition = mock(PlayerComposition.class);
		when(composition.getGender()).thenReturn(0);
		when(composition.getColors()).thenReturn(new int[5]);
		layers.deriveNonEquipment(composition, IdleAnimations.DEFAULT);
		layers.deriveEquipment(composition);

		RemoteData.ANIM_DATA.add(new AnimationData(TWO_HANDED_ANIM, "2h", null, null, null,
			Collections.singletonList(ItemID.WHITE_2H_SWORD)));
		ItemComposition sword = mock(ItemComposition.class);
		when(sword.getId()).thenReturn(ItemID.WHITE_2H_SWORD);
		when(sword.getMembersName()).thenReturn("White 2h sword");
		idleAnimations.queue(sword);

		layers.getVirtualModels().getItems().put(KitType.WEAPON, TestData.white2hSword);
	}

	@AfterEach
	void clearAnimData()
	{
		RemoteData.ANIM_DATA.clear();
		layers.resetPreview();
	}

	@Test
	void virtualWeaponSetsAnim()
	{
		assertEquals(TWO_HANDED_ANIM, layers.computeIdlePoseAnimation());
	}

	@Test
	void previewDisplacingVirtualWeaponUsesDefaultAnim()
	{
		layers.getPreviewModels().getItems().put(KitType.SHIELD, shield);
		assertEquals(IdleAnimations.DEFAULT, layers.computeIdlePoseAnimation());
	}
}
