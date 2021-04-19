package eq.uirs.fashionscape.panel;

import com.google.common.base.Objects;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.swap.SwapManager;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

/**
 * Tab panel that houses "base" player models and colors
 */
@Slf4j
public class KitsPanel extends JPanel
{
	private final SwapManager swapManager;
	private final ClientThread clientThread;
	private final Client client;

	private final JPanel resultsPanel = new JPanel();
	private final JScrollPane scrollPane = new JScrollPane();
	private final List<KitItemPanel> kitPanels = new ArrayList<>();

	private Boolean isFemale = null;
	private final KitColorOpener kitColorOpener = (slot, type) -> {
		kitPanels.forEach(panel -> {
			if (Objects.equal(slot, panel.getSlot()) && Objects.equal(type, panel.getType()))
			{
				Boolean female = isFemale();
				if (female != null)
				{
					panel.openOptions(female);
				}
			}
			else
			{
				panel.closeOptions();
			}
		});
		updateUI();
		scrollPane.revalidate();
	};

	@Value
	private static class KitColorResult
	{
		KitType slot;
		Integer kitId;

		ColorType colorType;
		Integer colorId;
	}

	@Inject
	public KitsPanel(SwapManager swapManager, ClientThread clientThread, Client client)
	{
		this.swapManager = swapManager;
		this.clientThread = clientThread;
		this.client = client;

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

	private void populateKitSlots()
	{
		clientThread.invokeLater(() -> {
			List<KitColorResult> results = new ArrayList<>();
			for (PanelKitSlot slot : PanelKitSlot.values())
			{
				KitType kitType = slot.getKitType();
				if (KitType.JAW.equals(kitType) && Objects.equal(isFemale(), true))
				{
					continue;
				}
				Integer kitId = null;
				if (kitType != null)
				{
					kitId = swapManager.swappedKitIdIn(kitType);
				}
				ColorType colorType = slot.getColorType();
				Integer colorId = null;
				if (colorType != null)
				{
					colorId = swapManager.swappedColorIdIn(colorType);
				}
				results.add(new KitColorResult(kitType, kitId, colorType, colorId));
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
			if (result.slot != null)
			{
				image = ImageUtil.loadImageResource(getClass(), result.slot.name().toLowerCase() + ".png");
			}
			KitItemPanel panel = new KitItemPanel(swapManager, result.colorType, result.colorId, result.slot,
				result.kitId, kitColorOpener, image, clientThread);
			kitPanels.add(panel);
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

	public void onPlayerChanged()
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			PlayerComposition composition = player.getPlayerComposition();
			if (composition != null)
			{
				boolean female = composition.isFemale();
				if (isFemale == null || female != isFemale)
				{
					isFemale = female;
					populateKitSlots();
				}
			}
		}
	}

	// returns null if gender cannot be determined
	@Nullable
	private Boolean isFemale()
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			PlayerComposition composition = player.getPlayerComposition();
			if (composition != null)
			{
				return composition.isFemale();
			}
		}
		return null;
	}
}
