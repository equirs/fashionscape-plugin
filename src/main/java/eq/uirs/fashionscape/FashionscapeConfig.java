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
		keyName = "randomizerIntelligence",
		name = "Randomizer intelligence",
		description = "Higher intelligence utilizes colour matching"
	)
	default RandomizerIntelligence randomizerIntelligence()
	{
		return RandomizerIntelligence.LOW;
	}

}
