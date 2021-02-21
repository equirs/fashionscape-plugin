package eq.uirs.fashionscape;

import eq.uirs.fashionscape.swap.RandomizerIntelligence;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("fashionscape")
public interface FashionscapeConfig extends Config
{

	String KEY_EXCLUDE_NON_STANDARD = "excludeNonStandardItems";

	@ConfigItem(
		position = 0,
		keyName = "color0",
		name = "Color 0",
		description = "debug override color 0"
	)
	default int color0()
	{
		return 1;
	}

	@ConfigItem(
		position = 1,
		keyName = "color1",
		name = "Color 1",
		description = "debug override color 1"
	)
	default int color1()
	{
		return 1;
	}

	@ConfigItem(
		position = 2,
		keyName = "color2",
		name = "Color 2",
		description = "debug override color 2"
	)
	default int color2()
	{
		return 1;
	}

	@ConfigItem(
		position = 3,
		keyName = "color3",
		name = "Color 3",
		description = "debug override color 3"
	)
	default int color3()
	{
		return 1;
	}

	@ConfigItem(
		position = 4,
		keyName = "color4",
		name = "Color 4",
		description = "debug override color 4"
	)
	default int color4()
	{
		return 1;
	}

	@ConfigItem(
		position = 5,
		keyName = KEY_EXCLUDE_NON_STANDARD,
		name = "Exclude non-standard items",
		description = "Filters out items that cannot be normally equipped anywhere"
	)
	default boolean excludeNonStandardItems()
	{
		return false;
	}

	@ConfigItem(
		position = 6,
		keyName = "randomizerIntelligence",
		name = "Randomizer intelligence",
		description = "Higher intelligence utilizes colour matching"
	)
	default RandomizerIntelligence randomizerIntelligence()
	{
		return RandomizerIntelligence.LOW;
	}

}
