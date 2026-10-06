package eq.uirs.fashionscape.core.loadout;

import com.google.common.collect.ImmutableSet;
import eq.uirs.fashionscape.core.event.ActiveLoadoutsChanged;
import eq.uirs.fashionscape.core.event.ColorChanged;
import eq.uirs.fashionscape.core.event.IconChanged;
import eq.uirs.fashionscape.core.event.ItemChanged;
import eq.uirs.fashionscape.core.event.KitChanged;
import eq.uirs.fashionscape.core.event.LoadoutsChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.PlayerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks which saved loadouts would leave the current look unchanged if applied.
 */
@Singleton
public class ActiveLoadouts
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final LoadoutManager loadoutManager;
	private final LoadoutStore store;

	@Getter
	private volatile Set<String> activeIds = ImmutableSet.of();
	private boolean checkPending;

	@Inject
	ActiveLoadouts(Client client, ClientThread clientThread, EventBus eventBus, LoadoutManager loadoutManager,
				   LoadoutStore store)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.loadoutManager = loadoutManager;
		this.store = store;
	}

	@Subscribe
	public void onLoadoutsChanged(LoadoutsChanged e)
	{
		scheduleCheck();
	}

	// kits resolve differently once the player's gender is known
	@Subscribe
	public void onPlayerChanged(PlayerChanged e)
	{
		if (e.getPlayer() == client.getLocalPlayer())
		{
			scheduleCheck();
		}
	}

	@Subscribe
	public void onItemChanged(ItemChanged e)
	{
		onModelChanged(e.getModelType());
	}

	@Subscribe
	public void onKitChanged(KitChanged e)
	{
		onModelChanged(e.getModelType());
	}

	@Subscribe
	public void onColorChanged(ColorChanged e)
	{
		onModelChanged(e.getModelType());
	}

	@Subscribe
	public void onIconChanged(IconChanged e)
	{
		onModelChanged(e.getModelType());
	}

	private void onModelChanged(ModelType type)
	{
		if (type == ModelType.VIRTUAL)
		{
			scheduleCheck();
		}
	}

	// applying a loadout changes many slots, so check once after all of them
	private synchronized void scheduleCheck()
	{
		if (checkPending)
		{
			return;
		}
		checkPending = true;
		clientThread.invokeLater(() -> {
			synchronized (this)
			{
				checkPending = false;
			}
			check();
		});
	}

	private void check()
	{
		Set<String> ids = Collections.unmodifiableSet(store.getAll().stream()
			.filter(s -> loadoutManager.isApplied(s.getLoadout()))
			.map(SavedLoadout::getId)
			.collect(Collectors.toSet()));
		if (!ids.equals(activeIds))
		{
			activeIds = ids;
			eventBus.post(new ActiveLoadoutsChanged(ids));
		}
	}
}
