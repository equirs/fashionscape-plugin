package eq.uirs.fashionscape;

import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.swap.RandomizerIntelligence;
import java.util.HashMap;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.apache.commons.lang3.SerializationUtils;

@ConfigGroup("fashionscape")
public interface FashionscapeConfig extends Config
{

	String KEY_EXCLUDE_NON_STANDARD = "excludeNonStandardItems";
	String KEY_IMPORT_MENU_ENTRY = "copyMenuEntry";

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
		keyName = KEY_IMPORT_MENU_ENTRY,
		name = "Copy menu entry",
		description = "Adds 'copy-outfit' menu option to other players"
	)
	default boolean copyMenuEntry()
	{
		return true;
	}

	@ConfigSection(
		name = "Randomizer Settings",
		description = "Settings relating to the outfit randomizer",
		position = 2
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
		description = "Randomizer will not shuffle base models if checked",
		section = randomizerSettings
	)
	default boolean excludeBaseModels()
	{
		return false;
	}

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
		keyName = "currentItems",
		name = "Current items",
		description = "The player's item ids set by the plugin (hidden)",
		hidden = true
	)
	default byte[] currentColors()
	{
		return SerializationUtils.serialize(new HashMap<ColorType, Integer>());
	}

	@ConfigItem(
		keyName = "currentItems",
		name = "Current items",
		description = "The player's item ids set by the plugin (hidden)",
		hidden = true
	)
	void setCurrentColors(byte[] colorMapBytes);

}
