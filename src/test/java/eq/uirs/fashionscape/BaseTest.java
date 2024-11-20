package eq.uirs.fashionscape;

import com.google.inject.testing.fieldbinder.Bind;
import eq.uirs.fashionscape.core.ConfigHelper;
import eq.uirs.fashionscape.core.IdleAnimations;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.TestData;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.model.Kits;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.remote.RemoteData;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import eq.uirs.fashionscape.util.BlockingExecutor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

public class BaseTest extends MockedTestBase
{
	@Bind
	protected FashionscapeConfig config = Mockito.mock(FashionscapeConfig.class);

	@Bind
	protected Client client = Mockito.mock(Client.class);

	@Bind
	protected ConfigManager configManager = Mockito.mock(ConfigManager.class);

	@Bind
	protected ScheduledExecutorService executor = new BlockingExecutor();

	@Bind
	protected ConfigHelper configHelper = Mockito.mock(ConfigHelper.class);

	@Bind
	protected RemoteDataHandler remote = Mockito.mock(RemoteDataHandler.class);

	@Bind
	protected Layers layers = new Layers(new IdleAnimations(executor));

	@BeforeAll
	public static void populate()
	{
		Map<Integer, ItemSlotInfo> info = RemoteData.ITEM_ID_TO_INFO;
		info.put(ItemID.RUNE_MED_HELM, convert(TestData.runeMedHelm));
		info.put(ItemID.SLAYER_FACEMASK, convert(TestData.faceMask));
		info.put(ItemID.IRON_FULL_HELM, convert(TestData.ironFullHelm));
		info.put(ItemID.CASTLEWARS_CLOAK_SARADOMIN, convert(TestData.hoodedCloak));
		info.put(ItemID.WHITE_2H_SWORD, convert(TestData.white2hSword));
		info.put(ItemID.BEACHPARTY_BOXINGGLOVES_YELLOW, convert(TestData.beachBoxingGloves));
		info.put(ItemID.BRONZE_PLATEBODY, convert(TestData.bronzePlatebody));
		info.put(ItemID.PLAGUE_JACKET, convert(TestData.plagueJacket));
		info.put(ItemID.GAUNTLET_PLATELEGS_T3_HM, convert(TestData.corruptedLegs));
	}

	private static ItemSlotInfo convert(SlotInfo i)
	{
		List<Integer> hides = i.getHidden().stream()
			.map(KitType::getIndex)
			.collect(Collectors.toList());
		return new ItemSlotInfo(
			i.getSlot().getIndex(),
			!hides.isEmpty() ? hides.get(0) : null,
			hides.size() > 1 ? hides.get(1) : null
		);
	}

	@AfterEach
	public void tearDown()
	{
		resetData();
	}

	protected void resetData()
	{
		Layers.resetModels(layers.getVirtualModels());
		layers.resetRealInfo();
	}

	protected void setKitsOnModels(Map<KitType, Integer> values, ModelInfo models)
	{
		Kits kits = models.getKits();
		values.forEach(kits::put);
	}
}
