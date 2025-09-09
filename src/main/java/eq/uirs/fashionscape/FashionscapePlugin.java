package eq.uirs.fashionscape;

import com.google.inject.Provides;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.data.anim.ItemInteractions;
import eq.uirs.fashionscape.panel.FashionscapePanel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Fashionscape",
	description = "Previews combinations of equipment by changing the player's local appearance"
)
@Slf4j
public class FashionscapePlugin extends Plugin
{
	public static final File OUTFITS_DIR = new File(RuneLite.RUNELITE_DIR, "outfits");
	public static final Pattern PROFILE_PATTERN = Pattern.compile("^(\\w+):(-?\\d+).*");
	private static final Pattern PAREN_REPLACE = Pattern.compile("\\(.*\\)");

	private static final String COPY_PLAYER = "Copy-outfit";
	private static final Set<Integer> ITEM_ID_DUPES = new HashSet<>();

	// combined set of all items to skip when searching (bad items, dupes, non-standard if applicable)
	public static Set<Integer> getItemIdsToExclude(FashionscapeConfig config)
	{
		Set<Integer> result = ITEM_ID_DUPES;
		result.addAll(ItemInteractions.BAD_ITEM_IDS);
		if (config.excludeNonStandardItems())
		{
			result.addAll(ItemInteractions.NON_STANDARD_ITEMS);
		}
		return result;
	}

	private static String stripName(String name)
	{
		String noParens = PAREN_REPLACE.matcher(name).replaceAll("");
		return noParens.replaceAll("[^A-Za-z]+", "");
	}

	@Value
	private static class ItemDupeData
	{
		int modelId;
		short[] colorsToReplace;
		short[] texturesToReplace;
		String strippedName;
	}

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private FashionManager fashionManager;

	@Inject
	private FashionscapeConfig config;

	@Inject
	private Provider<MenuManager> menuManager;

	private FashionscapePanel panel;
	private NavigationButton navButton;

	@Provides
	FashionscapeConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FashionscapeConfig.class);
	}

	@Override
	protected void startUp()
	{
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
		clientThread.invokeLater(() -> {
			populateDupes();
			fashionManager.startUp();
		});
	}

	@Override
	protected void shutDown()
	{
		menuManager.get().removePlayerMenuItem(COPY_PLAYER);
		clientThread.invokeLater(() -> fashionManager.shutDown());
		clientToolbar.removeNavigation(navButton);
		ITEM_ID_DUPES.clear();
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
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
		{
			fashionManager.onEquipmentChanged();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(FashionscapeConfig.GROUP))
		{
			if (event.getKey().equals(FashionscapeConfig.KEY_EXCLUDE_NON_STANDARD) ||
				event.getKey().equals(FashionscapeConfig.KEY_EXCLUDE_MEMBERS))
			{
				// reload displayed results
				clientThread.invokeLater(() -> {
					populateDupes();
					panel.reloadResults();
					panel.refreshKitsPanel();
				});
			}
			else if (event.getKey().equals(FashionscapeConfig.KEY_IMPORT_MENU_ENTRY))
			{
				refreshMenuEntries();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			populateDupes();
			fashionManager.onEquipmentChanged();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			fashionManager.setGender(null);
		}
		if (panel != null)
		{
			panel.onGameStateChanged(event);
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
			fashionManager.copyOutfit(p.getPlayerComposition());
			panel.reloadResults();
		}
	}

	private void populateDupes()
	{
		ITEM_ID_DUPES.clear();
		Set<Integer> ids = new HashSet<>();
		Set<ItemDupeData> itemUniques = new HashSet<>();
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
		for (int i = 0; i < client.getItemCount(); i++)
		{
			int canonical = itemManager.canonicalize(i);
			if (skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(canonical);
			String itemName = itemComposition.getMembersName().toLowerCase();
			boolean badItemName = ItemInteractions.BAD_ITEM_NAMES.contains(itemName);
			boolean membersObject = config.excludeMembersItems() && itemComposition.isMembers();
			if (badItemName || membersObject)
			{
				ITEM_ID_DUPES.add(canonical);
				continue;
			}
			ItemStats itemStats = itemManager.getItemStats(canonical);
			if (!ids.contains(itemComposition.getId()) && itemStats != null && itemStats.isEquipable())
			{
				ItemDupeData itemDupeData = new ItemDupeData(
					itemComposition.getInventoryModel(),
					itemComposition.getColorToReplaceWith(),
					itemComposition.getTextureToReplaceWith(),
					stripName(itemName)
				);
				if (itemUniques.contains(itemDupeData))
				{
					ITEM_ID_DUPES.add(canonical);
					continue;
				}
				itemUniques.add(itemDupeData);
				ids.add(itemComposition.getId());
			}
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
