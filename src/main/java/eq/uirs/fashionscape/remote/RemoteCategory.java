package eq.uirs.fashionscape.remote;

import com.google.gson.reflect.TypeToken;
import eq.uirs.fashionscape.colors.GenderItemColors;
import eq.uirs.fashionscape.data.ItemSlotInfo;
import eq.uirs.fashionscape.data.MiscData;
import eq.uirs.fashionscape.data.anim.AllAnimationData;
import java.lang.reflect.Type;
import java.util.Map;

public enum RemoteCategory
{
	SLOT, COLOR, ANIMATION, MISC;

	private static final String COLOR_JSON = "colors.json";
	private static final String SLOT_JSON = "slots.json";
	private static final String ANIM_JSON = "anims.json";
	private static final String MISC_JSON = "misc.json";

	String jsonFile()
	{
		switch (this)
		{
			case SLOT:
				return SLOT_JSON;
			case COLOR:
				return COLOR_JSON;
			case ANIMATION:
				return ANIM_JSON;
			case MISC:
				return MISC_JSON;
			default:
				return "";
		}
	}

	Type deserializedType()
	{
		switch (this)
		{
			case SLOT:
				//@formatter:off
				return new TypeToken<Map<Integer, ItemSlotInfo>>(){}.getType();
				//@formatter:on
			case COLOR:
				//@formatter:off
				return new TypeToken<Map<Integer, GenderItemColors>>(){}.getType();
				//@formatter:on
			case ANIMATION:
				return AllAnimationData.class;
			case MISC:
				return MiscData.class;
			default:
				return null;
		}
	}
}
