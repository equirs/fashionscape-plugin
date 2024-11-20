package eq.uirs.fashionscape.remote;

import eq.uirs.fashionscape.colors.GenderItemColors;
import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.data.MiscData;
import eq.uirs.fashionscape.data.anim.AllAnimationData;
import eq.uirs.fashionscape.data.anim.AnimationData;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoteData
{
	// item id -> equipped slot and hidden slots
	public static final Map<Integer, ItemSlotInfo> ITEM_ID_TO_INFO = new ConcurrentHashMap<>();
	public static final Map<Integer, GenderItemColors> ITEM_ID_TO_COLORS = new ConcurrentHashMap<>();
	public static final Queue<AnimationData> ANIM_DATA = new ConcurrentLinkedQueue<>();
	public static MiscData MISC_DATA = null;

	static void setItemInfo(Map<Integer, ItemSlotInfo> info)
	{
		ITEM_ID_TO_INFO.clear();
		ITEM_ID_TO_INFO.putAll(info);
	}

	static void setColors(Map<Integer, GenderItemColors> colors)
	{
		ITEM_ID_TO_COLORS.clear();
		ITEM_ID_TO_COLORS.putAll(colors);
	}

	static void setAnimations(AllAnimationData allData)
	{
		ANIM_DATA.clear();
		ANIM_DATA.addAll(allData.values);
	}

	static void setMiscData(MiscData miscData)
	{
		MISC_DATA = miscData;
	}
}
