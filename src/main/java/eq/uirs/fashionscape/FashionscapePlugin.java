package eq.uirs.fashionscape;

import com.google.inject.Provides;
import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.randomizer.Randomizer;
import eq.uirs.fashionscape.overlay.DebugOverlay;
import eq.uirs.fashionscape.panel.FashionscapePanel;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Fashionscape",
	description = "Previews combinations of equipment by changing the player's local appearance"
)
@Slf4j
public class FashionscapePlugin extends Plugin
{
	private static final String COPY_PLAYER = "Copy-fashion";

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FashionManager fashionManager;

	@Inject
	private Layers layers;

	@Inject
	private Randomizer randomizer;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private RemoteDataHandler remote;

	@Inject
	private DebugOverlay debugOverlay;

	@Inject
	@Named("developerMode")
	private boolean developerMode;

	private FashionscapePanel panel;
	private NavigationButton navButton;
	private boolean hasLoggedIn;

	@Provides
	FashionscapeConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FashionscapeConfig.class);
	}

	@Override
	protected void startUp()
	{
		remote.fetch();
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panelicon.png");
		panel = injector.getInstance(FashionscapePanel.class);
		navButton = NavigationButton.builder()
			.tooltip("Fashionscape")
			.icon(icon)
			.panel(panel)
			.priority(8)
			.build();
		clientToolbar.addNavigation(navButton);
		refreshMenuEntries();
		fashionManager.startUp();
		if (developerMode)
		{
			overlayManager.add(debugOverlay);
		}
	}

	@Override
	protected void shutDown()
	{

		remote.removeListeners();
		Events.removeListeners();
		menuManager.get().removePlayerMenuItem(COPY_PLAYER);
		clientThread.invokeLater(() -> fashionManager.shutDown());
		clientToolbar.removeNavigation(navButton);
		if (developerMode)
		{
			overlayManager.remove(debugOverlay);
		}
	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged event)
	{
		Player player = event.getPlayer();
		if (player != null && player == client.getLocalPlayer())
		{
			fashionManager.onPlayerChanged();
			if (panel != null)
			{
				panel.onPlayerChanged(player);
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		fashionManager.loadRSProfile();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(FashionscapeConfig.GROUP))
		{
			return;
		}
		if (event.getKey().equals(FashionscapeConfig.KEY_EXCLUDE_MEMBERS))
		{
			// reload displayed results
			clientThread.invokeLater(() -> {
				panel.reloadResults();
				panel.refreshKitsPanel();
				randomizer.repopulateMemo();
			});
		}
		else if (event.getKey().equals(FashionscapeConfig.KEY_IMPORT_MENU_ENTRY))
		{
			refreshMenuEntries();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			hasLoggedIn = true;
		}
		else if (hasLoggedIn && event.getGameState() == GameState.LOGIN_SCREEN)
		{
			layers.resetRealInfo();
			hasLoggedIn = false;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER && event.getMenuOption().equals(COPY_PLAYER))
		{
			Player p = client.getTopLevelWorldView().players().byIndex(event.getId());
			if (p == null)
			{
				return;
			}
			fashionManager.importPlayer(p);
			panel.reloadResults();
		}
	}

	private void refreshMenuEntries()
	{
		if (config.copyMenuEntry())
		{
			menuManager.get().addPlayerMenuItem(COPY_PLAYER);
		}
		else
		{
			menuManager.get().removePlayerMenuItem(COPY_PLAYER);
		}
	}

}
