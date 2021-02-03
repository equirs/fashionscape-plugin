package eq.uirs.fashionscape;

import com.google.inject.Provides;
import eq.uirs.fashionscape.data.ItemInteractions;
import eq.uirs.fashionscape.panel.FashionscapePanel;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.item.ItemStats;

@PluginDescriptor(
	name = "Fashionscape",
	description = "Previews combinations of equipment by changing the player's local appearance"
)
@Slf4j
public class FashionscapePlugin extends Plugin
{
	public static final File OUTFITS_DIR = new File(RuneLite.RUNELITE_DIR, "outfits");
	public static final Pattern PROFILE_PATTERN = Pattern.compile("^(\\w+):(\\d+).*");

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

	private static final String CONFIG_GROUP = "fashionscape";
	private static final Set<Integer> ITEM_ID_DUPES = new HashSet<>();

	@Value
	private static class ItemIcon
	{
		int modelId;
		short[] colorsToReplace;
		short[] texturesToReplace;
	}

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SwapManager swapManager;

	@Inject
	private FashionscapeConfig config;

	// Panel stuff
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

		swapManager.startUp();
		populateDupes();
	}

	@Override
	protected void shutDown()
	{
		swapManager.revertSwaps(true);
		swapManager.shutDown();
		clientToolbar.removeNavigation(navButton);
		ITEM_ID_DUPES.clear();
	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged event)
	{
		Player player = event.getPlayer();
		if (player != null && player == client.getLocalPlayer())
		{
			swapManager.checkForKitIds();
			swapManager.refreshItemSwaps();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			if (event.getKey().equals(FashionscapeConfig.KEY_EXCLUDE_NON_STANDARD))
			{
				// reload displayed results
				populateDupes();
				panel.getSearchPanel().reloadResults();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			populateDupes();
		}
		if (panel != null)
		{
			panel.onGameStateChanged(event);
		}
	}

	private void populateDupes()
	{
		ITEM_ID_DUPES.clear();
		Set<Integer> ids = new HashSet<>();
		Set<ItemIcon> itemIcons = new HashSet<>();
		Set<Integer> skips = FashionscapePlugin.getItemIdsToExclude(config);
		for (int i = 0; i < client.getItemCount(); i++)
		{
			int canonical = itemManager.canonicalize(i);
			if (skips.contains(canonical))
			{
				continue;
			}
			ItemComposition itemComposition = itemManager.getItemComposition(canonical);
			ItemStats itemStats = itemManager.getItemStats(canonical, false);
			if (!ids.contains(itemComposition.getId()) && itemStats != null && itemStats.isEquipable())
			{
				// Check if the results already contain the same item image
				ItemIcon itemIcon = new ItemIcon(itemComposition.getInventoryModel(),
					itemComposition.getColorToReplaceWith(), itemComposition.getTextureToReplaceWith());
				if (itemIcons.contains(itemIcon))
				{
					ITEM_ID_DUPES.add(canonical);
					continue;
				}
				itemIcons.add(itemIcon);
				ids.add(itemComposition.getId());
			}
		}
	}

}
