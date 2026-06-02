package eq.uirs.fashionscape.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Set;
import lombok.Value;

@Value
public class MiscData
{
	@Value
	public static class BadItemRange
	{
		@SerializedName("start")
		public int startInclusive;
		@SerializedName("end")
		public int endInclusive;
	}

	// broken items which have missing/placeholder models or are obsolete versions of other items
	@SerializedName("bad_item_ids")
	public Set<Integer> badItemIds;
	// continuous ranges of items that shouldn't be shown in results
	@SerializedName("bad_item_ranges")
	public List<BadItemRange> badItemRanges;
	// must match item name exactly
	@SerializedName("bad_item_names")
	public Set<String> badItemNames;
	// checks using String::contains
	@SerializedName("bad_item_contains")
	public Set<String> badItemContains;
	// checks using `find` on Pattern matcher
	@SerializedName("bad_item_regexes")
	public Set<String> badItemRegexes;

	/*
	 * item ids of weapon-slot equipment that, when detected in-game, will override the weapon and hide the shield.
	 * these are known as "interface items" and are temporarily wielded during animations
	 */
	@SerializedName("disable_anim_weapons")
	public Set<Integer> disableAnimWeapons;
	// item ids of weapon/shield-slot equipment that, when detected in-game, overrides weapon and shield
	@SerializedName("disable_anim_weapon_shield")
	public Set<Integer> disableAnimWeaponOrShield;
}
