package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.base.BaseLayersTest;
import eq.uirs.fashionscape.data.color.ColorType;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayersPreviewTest extends BaseLayersTest
{
	private final SlotInfo realHelm = SlotInfo.item(ItemID.RUNE_MED_HELM, KitType.HEAD);
	private final SlotInfo virtualHelm = SlotInfo.item(ItemID.IRON_FULL_HELM, KitType.HEAD);

	@BeforeEach
	void setUpPlayer()
	{
		PlayerComposition composition = mock(PlayerComposition.class);
		when(composition.getGender()).thenReturn(0);
		when(composition.getColors()).thenReturn(new int[5]);
		layers.deriveNonEquipment(composition, 0);
		layers.deriveEquipment(composition);

		layers.getRealModels().getItems().put(KitType.HEAD, realHelm);
		layers.getVirtualModels().getItems().put(KitType.HEAD, virtualHelm);
		layers.getVirtualModels().getColors().put(ColorType.SKIN, 5);
	}

	@AfterEach
	void resetPreview()
	{
		layers.resetPreview();
	}

	@Test
	void replacingPreviewHidesVirtualModels()
	{
		layers.setPreviewReplacesVirtual(true);
		assertEquals(realHelm.getEquipmentId(), layers.computeEquipment()[KitType.HEAD.getIndex()]);
		assertEquals(0, layers.computeColors()[ColorType.SKIN.ordinal()]);
	}

	@Test
	void resetShowsVirtualModelsAgain()
	{
		layers.setPreviewReplacesVirtual(true);
		layers.resetPreview();
		assertEquals(virtualHelm.getEquipmentId(), layers.computeEquipment()[KitType.HEAD.getIndex()]);
		assertEquals(5, layers.computeColors()[ColorType.SKIN.ordinal()]);
	}
}
