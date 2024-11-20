package eq.uirs.fashionscape.core.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

/**
 * A `true` event means that fallback kit is used instead of the player's known kit.
 * A `false` event means that the kit is definitively known by the plugin.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class KnownKitChanged extends FashionscapeEvent
{
	boolean unknown;
	KitType slot;
}
