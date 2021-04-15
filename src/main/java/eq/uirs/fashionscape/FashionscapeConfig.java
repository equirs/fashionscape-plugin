package eq.uirs.fashionscape;

import eq.uirs.fashionscape.swap.RandomizerIntelligence;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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

}
