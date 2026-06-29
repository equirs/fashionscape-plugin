package eq.uirs.fashionscape.base;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.google.inject.testing.fieldbinder.Bind;
import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.ConfigHelper;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.TestData;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.data.MiscData;
import eq.uirs.fashionscape.remote.RemoteData;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import eq.uirs.fashionscape.util.BlockingExecutor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.inject.Named;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;

public class BaseTest extends MockedTestBase
{
	@Bind
	protected ScheduledExecutorService executor = new BlockingExecutor();

	@Bind
	@Mock
	protected FashionscapeConfig config;

	@Bind
	@Mock
	protected Client client;

	@Bind
	@Mock
	protected ConfigManager configManager;

	@Bind
	@Mock
	protected ConfigHelper configHelper;

	@Bind
	@Mock
	protected RemoteDataHandler remote;

	@Bind
	@Mock
	protected EventBus eventBus;

	@Bind
	@Named("real")
	protected Provider<ModelInfo> realModels = () -> new ModelInfo(ModelType.REAL, eventBus);

	@Bind
	@Named("virtual")
	protected Provider<ModelInfo> virtualModels = () -> new ModelInfo(ModelType.VIRTUAL, eventBus);

	@Bind
	@Named("preview")
	protected Provider<ModelInfo> previewModels = () -> new ModelInfo(ModelType.PREVIEW, eventBus);

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

		RemoteData.MISC_DATA = new MiscData(
			new HashSet<>(),
			new ArrayList<>(),
			new HashSet<>(),
			new HashSet<>(),
			new HashSet<>(),
			ImmutableSet.of(ItemID.MAGIC_CARPET),
			new HashSet<>()
		);
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
}
