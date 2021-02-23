package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.Colorable;
import eq.uirs.fashionscape.data.Kit;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;

/**
 * Could represent either a player's kit, a color, or both for a given slot.
 */
@Slf4j
public class KitColorPanel extends AbsIconLabelPanel
{
	private static final Map<Integer, Kit> KIT_MAP = new HashMap<>();

	static
	{
		for (Kit value : Kit.values())
		{
			KIT_MAP.put(value.getKitId(), value);
		}
	}

	@Getter
	private final ColorType type;
	@Getter
	private final KitType slot;

	private final SwapManager swapManager;
	private final KitOpener kitOpener;
	private final ColorOpener colorOpener;

	private final Map<Integer, Colorable> colorMap = new HashMap<>();
	private final Map<KitType, List<Kit>> kitTypeMap = new HashMap<>();

	// constrain items in the list of dropdown results
	private final GridBagConstraints itemConstraints = new GridBagConstraints();
	private final JPanel optionsContainer = new JPanel();
	protected final JLabel kitIcon = new JLabel();
	protected final JLabel colorIcon = new JLabel();
	private final List<Kit> allKits = new ArrayList<>();

	private Integer colorId;
	private Integer kitId;

	public KitColorPanel(SwapManager swapManager, ColorType type, Integer colorId, KitType slot, Integer kitId,
						 KitOpener kitOpener, ColorOpener colorOpener, BufferedImage image, ClientThread clientThread)
	{
		super(image, clientThread);
		this.swapManager = swapManager;
		this.type = type;
		this.slot = slot;
		this.kitOpener = kitOpener;
		this.colorOpener = colorOpener;
		this.colorId = colorId;
		this.kitId = kitId;

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);

		itemConstraints.fill = GridBagConstraints.HORIZONTAL;
		itemConstraints.weightx = 1;
		itemConstraints.weighty = 0;
		itemConstraints.gridx = 0;
		itemConstraints.gridy = 0;

		JPanel icons = new JPanel();
		int cols = slot != null && type != null ? 2 : 1;
		icons.setLayout(new GridLayout(1, 2));

		if (slot != null)
		{
			allKits.clear();
			Arrays.stream(Kit.values())
				.filter(kit -> kit.getKitType().equals(slot))
				.forEach(allKits::add);
			setKitName(kitId);
//			icon.addMouseListener(new MouseAdapter()
//			{
//				@Override
//				public void mouseReleased(MouseEvent e)
//				{
//					log.info("open kit for {}", slot.name());
//					kitOpener.openKitFor(slot);
//				}
//			});
		}
		else if (type != null)
		{
			setColorName(colorId);
//			icon.addMouseListener(new MouseAdapter()
//			{
//				@Override
//				public void mouseReleased(MouseEvent e)
//				{
//					log.info("open color for {}", type.name());
//					colorOpener.openColorFor(type);
//				}
//			});
		}

		if (type != null)
		{
			// need a separate icon
			for (Colorable colorable : type.getColorables())
			{
				colorMap.put(colorable.getColorId(type), colorable);
			}
			setIconColor(colorId);
			this.icon.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					log.info("open color for {}", type.getDisplayName());
					colorOpener.openColorFor(type);
				}
			});
		}

		optionsContainer.setLayout(new GridBagLayout());
		optionsContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
		optionsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(optionsContainer, BorderLayout.SOUTH);
	}

	public void openKitOptions(boolean isFemale)
	{
		itemConstraints.gridy = 0;
		optionsContainer.removeAll();
		if (slot != null)
		{
			for (Kit kit : allKits)
			{
				if (kit.isFemale() != isFemale)
				{
					continue;
				}
				JLabel kitLabel = new JLabel();
				kitLabel.setText(kit.getDisplayName());
				kitLabel.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseReleased(MouseEvent e)
					{
						swapManager.swapKit(slot, kit.getKitId(), true);
					}
				});
				optionsContainer.add(kitLabel, itemConstraints);
				itemConstraints.gridy++;
			}
			optionsContainer.updateUI();
		}
	}

	public void openColorOptions()
	{
		itemConstraints.gridy = 0;
		optionsContainer.removeAll();
		if (type != null)
		{
			for (Colorable colorable : type.getColorables())
			{
				JLabel colorLabel = new JLabel();
				colorLabel.setText(colorable.getDisplayName());
				colorLabel.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseReleased(MouseEvent e)
					{
						swapManager.swapColor(type, colorable.getColorId(type), true);
					}
				});
				optionsContainer.add(colorLabel, itemConstraints);
				itemConstraints.gridy++;
			}
			optionsContainer.updateUI();
		}
	}

	public void closeOptions()
	{
		optionsContainer.removeAll();
	}

	private void setKitName(Integer kitId)
	{
		String kitName = "Nothing";
		if (kitId != null)
		{
			Kit kit = KIT_MAP.get(kitId);
			if (kit != null)
			{
				kitName = kit.getDisplayName();
			}
		}
		label.setText(kitName);
	}

	private void setColorName(Integer colorId)
	{
		String colorName = "None";
		if (colorId != null)
		{
			colorName = colorMap.get(colorId).getDisplayName();
		}
		label.setText(colorName);
	}

	private void setIconColor(Integer colorId)
	{
		String colorName = "None";
		Color color = ColorScheme.DARKER_GRAY_COLOR;
		if (colorId != null)
		{
			Colorable colorable = colorMap.get(colorId);
			if (colorable != null)
			{
				colorName = colorable.getDisplayName();
				color = colorable.getColor();
			}
		}
		icon.setToolTipText(colorName);
		icon.setBackground(color);
	}

}
