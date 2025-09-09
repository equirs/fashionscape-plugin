package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.FashionscapeConfig;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

/**
 * Tab panel that houses "base" player models and colors
 */
@Slf4j
public class KitsPanel extends JPanel
{
	private final FashionManager fashionManager;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final Client client;
	private final FashionscapeConfig config;

	private final JPanel resultsPanel = new JPanel();
	private final JScrollPane scrollPane = new JScrollPane();
	private final List<KitItemPanel> kitPanels = new ArrayList<>();
	private JawIconPanel jawIconPanel = null;

	private Integer gender;
	private final KitColorOpener kitColorOpener = (slot, type) -> {
		kitPanels.forEach(panel -> {
			if (slot == panel.getSlot() && type == panel.getType())
			{
				Integer gender = getGender();
				if (gender != null)
				{
					panel.openOptions(gender);
				}
			}
			else
			{
				panel.closeOptions();
			}
		});
		if (jawIconPanel != null)
		{
			if (slot == null && type == null)
			{
				jawIconPanel.openOptions();
			}
			else
			{
				jawIconPanel.closeOptions();
			}
		}
		updateUI();
		scrollPane.revalidate();
	};

	@Value
	private static class KitColorResult
	{
		KitType slot;
		Integer id; // icon id if icon, kit id otherwise

		ColorType colorType;
		Integer colorId;
	}

	@Inject
	public KitsPanel(FashionManager fashionManager, ClientThread clientThread, Client client, FashionscapeConfig config,
					 ItemManager itemManager)
	{
		this.fashionManager = fashionManager;
		this.clientThread = clientThread;
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;

		setLayout(new GridLayout(1, 1));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		resultsPanel.setLayout(new GridBagLayout());
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

		JPanel resultsWrapper = new JPanel(new BorderLayout());
		resultsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsWrapper.add(resultsPanel, BorderLayout.NORTH);

		scrollPane.setViewportView(resultsWrapper);
		add(scrollPane, BorderLayout.CENTER);
		populateKitSlots();
	}

	public void collapseOptions()
	{
		for (KitItemPanel kitPanel : kitPanels)
		{
			kitPanel.closeOptions();
		}
		updateUI();
		scrollPane.revalidate();
	}

	void populateKitSlots()
	{
		clientThread.invokeLater(() -> {
			List<KitColorResult> results = new ArrayList<>();
			for (PanelKitSlot slot : PanelKitSlot.values())
			{
				KitType kitType = slot.getKitType();
				ColorType colorType = slot.getColorType();
				Integer kitId = null;
				Integer colorId = null;

				if (kitType == null && colorType == null)
				{
					if (config.excludeNonStandardItems() || config.excludeMembersItems())
					{
						continue;
					}
					JawIcon jawIcon = fashionManager.swappedIcon();
					Integer iconId = jawIcon != null ? jawIcon.getId() : null;
					results.add(new KitColorResult(null, iconId, null, null));
				}
				else
				{
					if (kitType != null)
					{
						kitId = fashionManager.swappedKitIdIn(kitType);
					}
					if (colorType != null)
					{
						colorId = fashionManager.swappedColorIdIn(colorType);
					}
					results.add(new KitColorResult(kitType, kitId, colorType, colorId));
				}
			}
			SwingUtilities.invokeLater(() -> addKitSlotPanels(results));
		});
	}

	private void addKitSlotPanels(List<KitColorResult> results)
	{
		resultsPanel.removeAll();
		kitPanels.clear();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;

		for (KitColorResult result : results)
		{
			BufferedImage image = null;
			JPanel panel;
			if (result.colorType == null && result.slot == null)
			{
				image = ImageUtil.loadImageResource(getClass(), "icon.png");
				JawIconPanel jawPanel = new JawIconPanel(image, clientThread, fashionManager, itemManager, kitColorOpener,
					result.id);
				jawIconPanel = jawPanel;
				panel = jawPanel;
			}
			else
			{
				if (result.slot != null)
				{
					image = ImageUtil.loadImageResource(getClass(), result.slot.name().toLowerCase() + ".png");
				}
				KitItemPanel kitPanel = new KitItemPanel(fashionManager, result.colorType, result.colorId, result.slot,
					result.id, kitColorOpener, image, clientThread);
				kitPanels.add(kitPanel);
				panel = kitPanel;
			}
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));
			marginWrapper.add(panel, BorderLayout.NORTH);
			resultsPanel.add(marginWrapper, c);
			c.gridy++;
		}
		revalidate();
		repaint();
	}

	public void onPlayerChanged(Player player)
	{
		if (player != null)
		{
			PlayerComposition composition = player.getPlayerComposition();
			if (composition != null)
			{
				int gender = composition.getGender();
				if (this.gender == null || this.gender != gender)
				{
					this.gender = gender;
					populateKitSlots();
				}
			}
		}
	}

	// returns null if gender cannot be determined
	@Nullable
	private Integer getGender()
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			PlayerComposition composition = player.getPlayerComposition();
			if (composition != null)
			{
				return composition.getGender();
			}
		}
		return null;
	}
}
