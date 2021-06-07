package eq.uirs.fashionscape.swap.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.kit.KitType;

/**
 * A `true` event means that fallback kit is used in a swap (instead of the player's known kit).
 * A `false` event means that the kit is definitively known by the plugin.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class KnownKitChanged extends SwapEvent
{
	boolean unknown;
	KitType slot;
}
