package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.panel.search.FashionscapeSearchPanel;
import eq.uirs.fashionscape.panel.swap.FashionscapeSwapsPanel;
import eq.uirs.fashionscape.panel.swap.SearchOpener;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
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
	private final FashionscapeSwapsPanel swapsPanel;
	@Getter
	private final FashionscapeSearchPanel searchPanel;

	@RequiredArgsConstructor
	static class SearchClearingPanel extends JPanel
	{
		boolean shouldClearSearch = true;
		private final FashionscapeSearchPanel searchPanel;

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
	public FashionscapePanel(FashionscapeSearchPanel searchPanel, SwapManager swapManager, ItemManager itemManager,
							 Client client, ClientThread clientThread, ChatMessageManager chatMessageManager)
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
				// TODO maybe this should navigate back to the outfit tab after choosing?
			}
		};
		this.swapsPanel = new FashionscapeSwapsPanel(swapManager, itemManager, client, chatMessageManager,
			searchOpener, clientThread);
		this.searchPanel = searchPanel;

		MaterialTab swapsTab = new MaterialTab("Outfit", tabGroup, swapsPanel);
		searchTab = new MaterialTab("Search", tabGroup, searchPanel);

		// need some hacky listeners set up to clear search results when changing tabs
		searchTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = false;
			searchPanel.reloadResults();
			return true;
		});
		swapsTab.setOnSelectEvent(() -> {
			tabDisplayPanel.shouldClearSearch = true;
			return true;
		});

		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(swapsTab);
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
}

