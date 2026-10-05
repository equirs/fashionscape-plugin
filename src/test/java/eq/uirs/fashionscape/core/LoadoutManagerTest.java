package eq.uirs.fashionscape.core;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.HairKit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadoutManagerTest
{
	private static final int MASC = 0;
	private static final int FEM = 1;

	private final Layers layers = mock(Layers.class);
	private final Fallbacks fallbacks = mock(Fallbacks.class);
	private final ModelInfo virtual = new ModelInfo(ModelType.VIRTUAL, mock(EventBus.class));
	private LoadoutManager manager;

	@BeforeEach
	void setUp()
	{
		when(layers.getVirtualModels()).thenReturn(virtual);
		manager = new LoadoutManager(mock(ClientThread.class), mock(ChatMessageManager.class), layers,
			mock(Locks.class), mock(History.class), fallbacks);
	}

	@Test
	void resolvesKitsForGender()
	{
		Loadout loadout = kits(HairKit.BALD.getMascKitId());
		assertEquals(ImmutableMap.of(KitType.HAIR, HairKit.BALD.getFemKitId()), manager.resolveKits(loadout, FEM));
	}

	@Test
	void unknownGenderResolvesNoKits()
	{
		assertTrue(manager.resolveKits(kits(HairKit.BALD.getMascKitId()), null).isEmpty());
	}

	@Test
	void unknownKitResolvesToFallback()
	{
		when(fallbacks.getKit(eq(KitType.HAIR), anyInt(), anyBoolean())).thenReturn(HairKit.LONG.getFemKitId());
		assertEquals(ImmutableMap.of(KitType.HAIR, HairKit.LONG.getFemKitId()), manager.resolveKits(kits(-5), FEM));
	}

	@Test
	void appliedAcrossGenders()
	{
		when(layers.getGender()).thenReturn(FEM);
		virtual.getItems().put(KitType.HEAD, SlotInfo.item(1163, KitType.HEAD));
		virtual.getKits().put(KitType.HAIR, HairKit.BALD.getFemKitId());
		virtual.getColors().put(ColorType.SKIN, 4);

		Loadout loadout = kits(HairKit.BALD.getMascKitId());
		loadout.getItems().put(KitType.HEAD, 1163);
		loadout.getColors().put(ColorType.SKIN, 4);
		assertTrue(manager.isApplied(loadout));

		loadout.getColors().put(ColorType.SKIN, 5);
		assertFalse(manager.isApplied(loadout));
	}

	@Test
	void notAppliedWhenLookHasMore()
	{
		when(layers.getGender()).thenReturn(MASC);
		virtual.getKits().put(KitType.HAIR, HairKit.BALD.getMascKitId());
		virtual.getKits().put(KitType.JAW, 10);
		assertFalse(manager.isApplied(kits(HairKit.BALD.getMascKitId())));
	}

	private static Loadout kits(int hairKitId)
	{
		Loadout loadout = new Loadout();
		loadout.getKits().put(KitType.HAIR, hairKitId);
		return loadout;
	}
}
