package eq.uirs.fashionscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("fashionscape")
public interface FashionscapeConfig extends Config
{

	String KEY_EXCLUDE_NON_STANDARD = "excludeNonStandardItems";

	@ConfigItem(
		position = 1,
		keyName = KEY_EXCLUDE_NON_STANDARD,
		name = "Exclude non-standard items",
		description = "Filters out items that cannot be normally equipped anywhere"
	)
	default boolean excludeNonStandardItems()
	{
		return false;
	}

}
