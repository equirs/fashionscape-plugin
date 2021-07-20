package eq.uirs.fashionscape.data.kit;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

/**
 * Unlike other kits, the jaw slot can contain icon items, which require special handling since they're treated
 * as neither normal items nor normal kits by the plugin.
 */
@RequiredArgsConstructor
public enum JawKit implements Kit
{
	NO_JAW("(no jaw)", true, -256,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 10556)
			.put(JawIcon.BA_DEFENDER, 10558)
			.put(JawIcon.BA_COLLECTOR, 10557)
			.put(JawIcon.BA_HEALER, 10559)
			.put(JawIcon.SW_BLUE, 25212)
			.put(JawIcon.SW_RED, 25228)
			.build()),

	GOATEE("Goatee", false, 10,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23460)
			.put(JawIcon.BA_DEFENDER, 23466)
			.put(JawIcon.BA_COLLECTOR, 22339)
			.put(JawIcon.BA_HEALER, 23478)
			.put(JawIcon.SW_BLUE, 25213)
			.put(JawIcon.SW_RED, 25229)
			.build()),

	LONG_J("Long", false, 11,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22723)
			.put(JawIcon.BA_DEFENDER, 22345)
			.put(JawIcon.BA_COLLECTOR, 23471)
			.put(JawIcon.BA_HEALER, 22311)
			.put(JawIcon.SW_BLUE, 25214)
			.put(JawIcon.SW_RED, 25230)
			.build()),

	MEDIUM_J("Medium", false, 12,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23461)
			.put(JawIcon.BA_DEFENDER, 22728)
			.put(JawIcon.BA_COLLECTOR, 23472)
			.put(JawIcon.BA_HEALER, 23479)
			.put(JawIcon.SW_BLUE, 25215)
			.put(JawIcon.SW_RED, 25231)
			.build()),

	SMALL_MOUSTACHE("Small moustache", false, 13,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22722)
			.put(JawIcon.BA_DEFENDER, 22344)
			.put(JawIcon.BA_COLLECTOR, 22338)
			.put(JawIcon.BA_HEALER, 22310)
			.put(JawIcon.SW_BLUE, 25216)
			.put(JawIcon.SW_RED, 25232)
			.build()),

	CLEAN_SHAVEN("Clean-shaven", false, 14,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23462)
			.put(JawIcon.BA_DEFENDER, 23467)
			.put(JawIcon.BA_COLLECTOR, 23473)
			.put(JawIcon.BA_HEALER, 23480)
			.put(JawIcon.SW_BLUE, 25217)
			.put(JawIcon.SW_RED, 25233)
			.build()),

	SHORT_J("Short", false, 15,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23463)
			.put(JawIcon.BA_DEFENDER, 23468)
			.put(JawIcon.BA_COLLECTOR, 22337)
			.put(JawIcon.BA_HEALER, 22309)
			.put(JawIcon.SW_BLUE, 25218)
			.put(JawIcon.SW_RED, 25234)
			.build()),

	POINTY("Pointy", false, 16,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22721)
			.put(JawIcon.BA_DEFENDER, 22343)
			.put(JawIcon.BA_COLLECTOR, 23474)
			.put(JawIcon.BA_HEALER, 23481)
			.put(JawIcon.SW_BLUE, 25219)
			.put(JawIcon.SW_RED, 25235)
			.build()),

	SPLIT("Split", false, 17,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23464)
			.put(JawIcon.BA_DEFENDER, 23469)
			.put(JawIcon.BA_COLLECTOR, 22315)
			.put(JawIcon.BA_HEALER, 23482)
			.put(JawIcon.SW_BLUE, 25220)
			.put(JawIcon.SW_RED, 25236)
			.build()),

	HANDLEBAR("Handlebar", false, 111,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22349)
			.put(JawIcon.BA_DEFENDER, 22342)
			.put(JawIcon.BA_COLLECTOR, 23475)
			.put(JawIcon.BA_HEALER, 22308)
			.put(JawIcon.SW_BLUE, 25221)
			.put(JawIcon.SW_RED, 25237)
			.build()),

	MUTTON("Mutton", false, 112,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22730)
			.put(JawIcon.BA_DEFENDER, 23470)
			.put(JawIcon.BA_COLLECTOR, 22314)
			.put(JawIcon.BA_HEALER, 23483)
			.put(JawIcon.SW_BLUE, 25222)
			.put(JawIcon.SW_RED, 25238)
			.build()),

	FULL_MUTTON("Full mutton", false, 113,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22348)
			.put(JawIcon.BA_DEFENDER, 22341)
			.put(JawIcon.BA_COLLECTOR, 23476)
			.put(JawIcon.BA_HEALER, 20802)
			.put(JawIcon.SW_BLUE, 25223)
			.put(JawIcon.SW_RED, 25239)
			.build()),

	BIG_MOUSTACHE("Big moustache", false, 114,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22729)
			.put(JawIcon.BA_DEFENDER, 22727)
			.put(JawIcon.BA_COLLECTOR, 22313)
			.put(JawIcon.BA_HEALER, 23484)
			.put(JawIcon.SW_BLUE, 25224)
			.put(JawIcon.SW_RED, 25240)
			.build()),

	WAXED_MOUSTACHE("Waxed moustache", false, 115,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22347)
			.put(JawIcon.BA_DEFENDER, 22340)
			.put(JawIcon.BA_COLLECTOR, 22724)
			.put(JawIcon.BA_HEALER, 10567)
			.put(JawIcon.SW_BLUE, 25225)
			.put(JawIcon.SW_RED, 25241)
			.build()),

	DALI("Dali", false, 116,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 23465)
			.put(JawIcon.BA_DEFENDER, 22726)
			.put(JawIcon.BA_COLLECTOR, 22312)
			.put(JawIcon.BA_HEALER, 23485)
			.put(JawIcon.SW_BLUE, 25226)
			.put(JawIcon.SW_RED, 25242)
			.build()),

	VIZIER("Vizier", false, 117,
		new ImmutableMap.Builder<JawIcon, Integer>()
			.put(JawIcon.BA_ATTACKER, 22346)
			.put(JawIcon.BA_DEFENDER, 22725)
			.put(JawIcon.BA_COLLECTOR, 23477)
			.put(JawIcon.BA_HEALER, 23486)
			.put(JawIcon.SW_BLUE, 25227)
			.put(JawIcon.SW_RED, 25243)
			.build());

	// item id -> icon
	private static final Map<Integer, JawIcon> reverseLookupIcon = new HashMap<>();
	// equipment id -> kit
	private static final Map<Integer, JawKit> reverseLookupKit = new HashMap<>();

	@Nonnull
	public static JawIcon iconFromItemId(Integer itemId)
	{
		JawIcon icon = reverseLookupIcon.get(itemId);
		return icon != null ? icon : JawIcon.NOTHING;
	}

	public static boolean isNoJawIcon(int itemId)
	{
		return JawKit.NO_JAW.icons.containsValue(itemId);
	}

	public static JawKit fromEquipmentId(int equipId)
	{
		return reverseLookupKit.get(equipId);
	}

	static
	{
		for (JawKit kit : JawKit.values())
		{
			reverseLookupKit.put(kit.kitId + 256, kit);
			kit.icons.forEach((icon, itemId) -> {
				reverseLookupIcon.put(itemId, icon);
				reverseLookupKit.put(itemId + 512, kit);
			});
		}
	}

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	// note: since they're items, these are all item ids
	private final Map<JawIcon, Integer> icons;

	@Override
	public KitType getKitType()
	{
		return KitType.JAW;
	}

	@Override
	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public boolean isFemale()
	{
		return isFemale;
	}

	@Override
	public int getKitId()
	{
		return kitId;
	}

	@Nullable
	public Integer getIconItemId(JawIcon icon)
	{
		return icons.get(icon);
	}
}
