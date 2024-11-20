package eq.uirs.fashionscape;

import eq.uirs.fashionscape.core.LockStatus;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.randomizer.RandomizerIntelligence;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.panel.SortBy;
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
	String KEY_EXCLUDE_MEMBERS = "excludeMembersItems";
	String KEY_IMPORT_MENU_ENTRY = "copyMenuEntry";
	String KEY_REAL_KITS = "realKitIds";

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

	// replaces older currentEquipment key
	@ConfigItem(
		keyName = "currentEquipmentInfo",
		name = "Current equipment info",
		description = "The player's virtual equipment info (hidden)",
		hidden = true
	)
	default byte[] equipmentInfo()
	{
		return SerializationUtils.serialize(new HashMap<KitType, SlotInfo>());
	}

	@ConfigItem(
		keyName = "currentEquipmentInfo",
		name = "Current equipment info",
		description = "The player's virtual equipment info (hidden)",
		hidden = true
	)
	void setEquipmentInfo(byte[] equipIdsMapBytes);

	@ConfigItem(
		keyName = "currentEquipment",
		name = "Legacy equipment",
		description = "Deprecated config, migrated to currentEquipmentInfo",
		hidden = true
	)
	default byte[] legacyEquipment()
	{
		return SerializationUtils.serialize(new HashMap<KitType, Integer>());
	}

	@ConfigItem(
		keyName = "currentEquipment",
		name = "Legacy equipment",
		description = "Deprecated config, migrated to currentEquipmentInfo",
		hidden = true
	)
	void setLegacyEquipment(byte[] equipmentIds);

	@ConfigItem(
		keyName = "currentIcon",
		name = "Current icon",
		description = "The player's jaw icon set by the plugin (hidden)",
		hidden = true
	)
	default Integer icon()
	{
		return null;
	}

	@ConfigItem(
		keyName = "currentIcon",
		name = "Current icon",
		description = "The player's jaw icon set by the plugin (hidden)",
		hidden = true
	)
	void setIcon(Integer iconId);

	@ConfigItem(
		keyName = "currentColors",
		name = "Current colors",
		description = "The player's color ids set by the plugin (hidden)",
		hidden = true
	)
	default byte[] colors()
	{
		return SerializationUtils.serialize(new HashMap<ColorType, Integer>());
	}

	@ConfigItem(
		keyName = "currentColors",
		name = "Current colors",
		description = "The player's color ids set by the plugin (hidden)",
		hidden = true
	)
	void setColors(byte[] colorMapBytes);

	@ConfigItem(
		keyName = "currentLocks",
		name = "Current locks",
		description = "The player's lock statuses set by the plugin (hidden)",
		hidden = true
	)
	default byte[] locks()
	{
		return SerializationUtils.serialize(new HashMap<KitType, LockStatus>());
	}

	@ConfigItem(
		keyName = "currentLocks",
		name = "Current locks",
		description = "The player's lock statuses set by the plugin (hidden)",
		hidden = true
	)
	void setLocks(byte[] locks);

	@ConfigItem(
		keyName = "currentColorLocks",
		name = "Current color locks",
		description = "The player's color lock statuses set by the plugin (hidden)",
		hidden = true
	)
	default byte[] colorLocks()
	{
		return SerializationUtils.serialize(new HashMap<ColorType, Boolean>());
	}

	@ConfigItem(
		keyName = "currentColorLocks",
		name = "Current color locks",
		description = "The player's color lock statuses set by the plugin (hidden)",
		hidden = true
	)
	void setColorLocks(byte[] colorLocks);

	@ConfigItem(
		keyName = "currentIconLocked",
		name = "Current icon locked",
		description = "The player's icon lock status set by the plugin (hidden)",
		hidden = true
	)
	default boolean iconLocked()
	{
		return false;
	}

	@ConfigItem(
		keyName = "currentIconLocked",
		name = "Current icon locked",
		description = "The player's icon lock status set by the plugin (hidden)",
		hidden = true
	)
	void setIconLocked(boolean value);

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
