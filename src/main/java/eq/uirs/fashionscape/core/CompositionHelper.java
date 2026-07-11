package eq.uirs.fashionscape.core;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;

/**
 * Because fashionscape only operates on non-transformed players,
 * calls to get players' compositions is wrapped in this class,
 * which discards compositions if there's a non-default TransformedNpcId.
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CompositionHelper
{
	private final Client client;

	@Nullable
	public PlayerComposition getLocal()
	{
		return get(client.getLocalPlayer());
	}

	public PlayerComposition get(@Nullable Player player)
	{
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null || composition.getTransformedNpcId() != -1)
		{
			return null;
		}
		return composition;
	}
}
