package eq.uirs.fashionscape;

import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.panel.SortBy;
import eq.uirs.fashionscape.swap.RandomizerIntelligence;
import java.util.HashMap;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.apache.commons.lang3.SerializationUtils;

@ConfigGroup(FashionscapeConfig.GROUP)
public interface FashionscapeConfig extends Config
{
	String GROUP = "fashionscape";
	String KEY_EXCLUDE_NON_STANDARD = "excludeNonStandardItems";
	String KEY_EXCLUDE_MEMBERS = "excludeMembersItems";
	String KEY_IMPORT_MENU_ENTRY = "copyMenuEntry";
	String KEY_REAL_KITS = "realKitIds";

	@ConfigItem(
		position = -1,
		keyName = "petId",
		name = "Pet ID",
		description = "Pet npc id to transform to"
	)
	default int petId()
	{
		return 1;
	}

	@ConfigItem(
		position = -1,
		keyName = "petAnim",
		name = "Pet pose anim ID",
		description = "Pet animation to set"
	)
	default int petAnim()
	{
		return 1;
	}

	@ConfigItem(
		position = -1,
		keyName = "recolor",
		name = "Recolor id",
		description = "Recolor to set"
	)
	default int recolor() {
		return 1;
	}

	@ConfigItem(
		position = 0,
		keyName = KEY_EXCLUDE_NON_STANDARD,
		name = "Exclude non-standard items",
		description = "Filters out items that cannot be normally equipped anywhere"
	)
	default boolean excludeNonStandardItems()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = KEY_EXCLUDE_MEMBERS,
		name = "Exclude members items",
		description = "Filters out members-only objects"
	)
	default boolean excludeMembersItems()
	{
		return false;
	}

	@ConfigItem(
		position = 2,
		keyName = KEY_IMPORT_MENU_ENTRY,
		name = "\"Copy-outfit\" entry",
		description = "Adds \"copy-outfit\" right click option to other players"
	)
	default boolean copyMenuEntry()
	{
		return true;
	}

	@ConfigSection(
		name = "Randomizer",
		description = "Settings relating to the outfit randomizer",
		position = 3
	)
	String randomizerSettings = "randomizerSettings";

	@ConfigItem(
		position = 0,
		keyName = "randomizerIntelligence",
		name = "Intelligence",
		description = "Randomizer will utilize colour matching with higher settings",
		section = randomizerSettings
	)
	default RandomizerIntelligence randomizerIntelligence()
	{
		return RandomizerIntelligence.LOW;
	}

	@ConfigItem(
		position = 1,
		keyName = "excludeBaseModels",
		name = "Exclude base models",
		description = "Randomizer will not shuffle base models (e.g., hair) if checked",
		section = randomizerSettings
	)
	default boolean excludeBaseModels()
	{
		return false;
	}

	// region Hidden stuff

	@ConfigItem(
		keyName = "preferredSort",
		name = "Preferred sort order",
		description = "Last used result sort order (hidden)",
		hidden = true
	)
	default SortBy preferredSort()
	{
		return SortBy.COLOR_MATCH;
	}

	@ConfigItem(
		keyName = "preferredSort",
		name = "Preferred sort order",
		description = "Last used result sort order (hidden)",
		hidden = true
	)
	void setPreferredSort(SortBy sort);

	@ConfigItem(
		keyName = "currentEquipment",
		name = "Current equipment",
		description = "The player's equipment ids set by the plugin (hidden)",
		hidden = true
	)
	default byte[] currentEquipment()
	{
		return SerializationUtils.serialize(new HashMap<KitType, Integer>());
	}

	@ConfigItem(
		keyName = "currentEquipment",
		name = "Current equipment",
		description = "The player's equipment ids set by the plugin (hidden)",
		hidden = true
	)
	void setCurrentEquipment(byte[] equipIdsMapBytes);

	@ConfigItem(
		keyName = "currentIcon",
		name = "Current icon",
		description = "The player's jaw icon set by the plugin (hidden)",
		hidden = true
	)
	default Integer currentIcon()
	{
		return null;
	}

	@ConfigItem(
		keyName = "currentIcon",
		name = "Current icon",
		description = "The player's jaw icon set by the plugin (hidden)",
		hidden = true
	)
	void setCurrentIcon(Integer iconId);

	@ConfigItem(
		// key name is not very accurate, oops
		keyName = "currentItems",
		name = "Current colors",
		description = "The player's color ids set by the plugin (hidden)",
		hidden = true
	)
	default byte[] currentColors()
	{
		return SerializationUtils.serialize(new HashMap<ColorType, Integer>());
	}

	@ConfigItem(
		// key name is not very accurate, oops
		keyName = "currentItems",
		name = "Current colors",
		description = "The player's color ids set by the plugin (hidden)",
		hidden = true
	)
	void setCurrentColors(byte[] colorMapBytes);

	@ConfigItem(
		keyName = KEY_REAL_KITS,
		name = "Real kit ids",
		description = "Known kit ids of the current player (hidden, per-profile)",
		hidden = true
	)
	default byte[] realKitIds()
	{
		return SerializationUtils.serialize(new HashMap<KitType, Integer>());
	}

	// endregion

}
