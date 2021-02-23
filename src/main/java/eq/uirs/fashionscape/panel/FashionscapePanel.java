package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

public class FashionscapePanel extends PluginPanel
{

	private final SearchClearingPanel tabDisplayPanel;
	private final MaterialTabGroup tabGroup;
	private final MaterialTab searchTab;

	@Getter
	private final SwapsPanel swapsPanel;
	@Getter
	private final SearchPanel searchPanel;
	@Getter
	private final KitsPanel kitsPanel;

	@RequiredArgsConstructor
	static class SearchClearingPanel extends JPanel
	{
		boolean shouldClearSearch = true;
		private final SearchPanel searchPanel;

		@Override
		public void removeAll()
		{
			if (shouldClearSearch)
			{
				searchPanel.clearResults();
			}
			super.removeAll();
		}
	}

	@Inject
	public FashionscapePanel(SearchPanel searchPanel, KitsPanel kitsPanel,
							 SwapManager swapManager, ItemManager itemManager, Client client,
							 ClientThread clientThread, ChatMessageManager chatMessageManager)
	{
		super(false);
		tabDisplayPanel = new SearchClearingPanel(searchPanel);
		tabGroup = new MaterialTabGroup(tabDisplayPanel);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		SearchOpener searchOpener = new SearchOpener()
		{
			@Override
			public void openSearchFor(KitType slot)
			{
				tabGroup.select(searchTab);
				searchPanel.chooseSlot(slot);
				searchPanel.clearSearch();
			}
		};
		this.swapsPanel = new SwapsPanel(swapManager, itemManager, client, chatMessageManager,
			searchOpener, clientThread);
		this.searchPanel = searchPanel;
		this.kitsPanel = kitsPanel;

		MaterialTab swapsTab = new MaterialTab("Outfit", tabGroup, swapsPanel);
		MaterialTab kitsTab = new MaterialTab("Base", tabGroup, kitsPanel);
		searchTab = new MaterialTab("Search", tabGroup, searchPanel);

		// need some hacky listeners set up to clear search results when changing tabs
		searchTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = false;
			searchPanel.reloadResults();
			return true;
		});
		kitsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			return true;
		});
		swapsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			return true;
		});

		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(swapsTab);
		tabGroup.addTab(kitsTab);
		tabGroup.addTab(searchTab);
		tabGroup.select(swapsTab);

		add(tabGroup, BorderLayout.NORTH);
		add(tabDisplayPanel, BorderLayout.CENTER);
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		if (swapsPanel != null)
		{
			swapsPanel.onGameStateChanged(event);
		}
	}

	public void onPlayerChanged(PlayerChanged event)
	{
		if (kitsPanel != null)
		{
			kitsPanel.onPlayerChanged(event);
		}
	}

	public void reloadResults()
	{
		if (searchPanel != null)
		{
			searchPanel.reloadResults();
		}
	}
}

