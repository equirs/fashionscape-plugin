package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.event.ColorChangedListener;
import eq.uirs.fashionscape.core.event.ColorLockChangedListener;
import eq.uirs.fashionscape.core.event.ItemChangedListener;
import eq.uirs.fashionscape.core.event.KitChangedListener;
import eq.uirs.fashionscape.core.event.LockChangedListener;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import eq.uirs.fashionscape.data.kit.Kit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.util.Objects;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

/**
 * Could represent either a player's kit, a color, or both for a given slot.
 */
@Slf4j
public class KitPanel extends DropdownIconPanel
{
	private static final Dimension COLOR_CHOOSER_SIZE = new Dimension(15, 15);

	@Getter
	@Nullable
	private final ColorType type;

	@Getter
	@Nullable
	private final KitType slot;

	private final FashionManager fashionManager;
	private final KitColorOpener kitColorOpener;

	private final Map<Integer, Colorable> colorMap = new HashMap<>();

	private final JButton lockColorButton = new JButton();
	private final List<Kit> allKits = new ArrayList<>();

	private Color color;

	private final KitChangedListener kitChangedListener;
	private final ItemChangedListener itemChangedListener;
	private final ColorChangedListener colorChangedListener;
	private final LockChangedListener lockChangedListener;
	private final ColorLockChangedListener colorLockChangedListener;

	public KitPanel(FashionManager fashionManager, @Nullable ColorType type, @Nullable KitType slot,
					KitColorOpener kitColorOpener, BufferedImage image, ClientThread clientThread)
	{
		super(image, clientThread);
		this.fashionManager = fashionManager;
		this.kitColorOpener = kitColorOpener;
		this.slot = slot;
		this.type = type;
		this.kitChangedListener = new KitChangedListener(e -> {
			if (e.getModelType() == ModelType.VIRTUAL && Objects.equals(slot, e.getSlot()))
			{
				setKitName();
				updateXButton();
			}
		});
		this.itemChangedListener = new ItemChangedListener(e -> {
			if (e.getModelType() == ModelType.VIRTUAL && Objects.equals(slot, e.getSlot()))
			{
				SlotInfo newInfo = e.getNewInfo();
				if (newInfo == null || newInfo.isNothing())
				{
					setKitName();
					updateXButton();
				}
			}
		});
		this.colorChangedListener = new ColorChangedListener(e -> {
			if (e.getModelType() == ModelType.VIRTUAL && Objects.equals(type, e.getType()))
			{
				setIconColor();
				if (slot == null)
				{
					setColorName();
				}
				setIconTooltip();
				updateXButton();
			}
		});
		this.lockChangedListener = new LockChangedListener(e -> {
			if (Objects.equals(slot, e.getSlot()))
			{
				updateLockButton();
			}
		});
		this.colorLockChangedListener = new ColorLockChangedListener(e -> {
			if (Objects.equals(type, e.getType()))
			{
				updateColorLockButton();
			}
		});

		if (type != null)
		{
			for (Colorable colorable : type.getColorables())
			{
				colorMap.put(colorable.getColorId(type), colorable);
			}
		}

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);

		int cols = type != null && slot != null ? 3 : 2;
		JPanel buttons = new JPanel(new GridLayout(1, cols, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (slot != null)
		{
			allKits.clear();
			allKits.addAll(Arrays.asList(Kit.allInSlot(slot, false)));
			setIcon(icon, image);
			configureButton(lockButton);
			lockButton.addActionListener(e -> fashionManager.toggleKitLocked(slot));
			buttons.add(lockButton);
		}
		else if (type != null)
		{
			BufferedImage colorImage = ImageUtil.loadImageResource(this.getClass(), "color.png");
			setIcon(icon, colorImage);
		}

		if (type != null)
		{
			configureButton(lockColorButton);
			lockColorButton.addActionListener(e -> fashionManager.toggleColorLocked(type));
			buttons.add(lockColorButton);
		}
		else
		{
			icon.setBorder(new RoundedBorder(ColorScheme.LIGHT_GRAY_COLOR, ICON_CORNER_RADIUS));
		}

		configureButton(xButton);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> fashionManager.revert(slot, type)));
		buttons.add(xButton);

		optionsContainer.setLayout(new BorderLayout());
		optionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		optionsContainer.setVisible(false);
		add(optionsContainer, BorderLayout.SOUTH);
		rightPanel.add(buttons, BorderLayout.EAST);
		add(rightPanel, BorderLayout.CENTER);
	}

	@Override
	void addListeners()
	{
		refreshData();

		Events.addListener(kitChangedListener);
		Events.addListener(itemChangedListener);
		Events.addListener(colorChangedListener);
		Events.addListener(lockChangedListener);
		Events.addListener(colorLockChangedListener);
	}

	@Override
	void removeListeners()
	{
		Events.removeListener(kitChangedListener);
		Events.removeListener(itemChangedListener);
		Events.removeListener(colorChangedListener);
		Events.removeListener(lockChangedListener);
		Events.removeListener(colorLockChangedListener);
	}

	private void refreshData()
	{
		if (slot != null)
		{
			setKitName();
		}
		else if (type != null)
		{
			setColorName();
		}
		setIconColor();
		setIconTooltip();
		updateLockButton();
		updateColorLockButton();
		updateXButton();
		// icon image for kits is never updated
	}

	@Override
	void openDropdown(MouseEvent e)
	{
		kitColorOpener.openOptions(slot, type);
	}

	@Override
	void tryClear()
	{
		fashionManager.overrideKitWithNothing(slot);
	}

	public void openOptions(Integer gender)
	{
		optionsContainer.removeAll();
		if (optionsContainer.isVisible())
		{
			optionsContainer.setVisible(false);
			return;
		}
		else
		{
			optionsContainer.setVisible(true);
		}

		JPanel colorsBox = new JPanel();
		colorsBox.setLayout(new GridBagLayout());
		colorsBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		if (type != null)
		{
			colorsBox.setBorder(new EmptyBorder(10, 0, 0, 0));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.LINE_START;
			c.weightx = 0;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;

			for (Colorable colorable : type.getColorables())
			{
				JLabel colorLabel = createColorLabel(colorable);

				JPanel colorWrapper = new JPanel(new BorderLayout());
				colorWrapper.setBorder(new EmptyBorder(1, 1, 1, 1));
				colorWrapper.add(colorLabel, BorderLayout.CENTER);

				colorsBox.add(colorWrapper, c);
				if (c.gridx == 9)
				{
					c.gridx = 0;
					c.gridy++;
				}
				else
				{
					c.gridx++;
				}
			}
		}
		optionsContainer.add(colorsBox, BorderLayout.NORTH);

		JPanel kitsList = new JPanel();
		kitsList.setLayout(new GridBagLayout());
		kitsList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		if (slot != null)
		{
			kitsList.setBorder(new EmptyBorder(10, 0, 0, 0));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.LINE_START;
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 0;
			c.gridy = 0;

			for (Kit kit : allKits)
			{
				final Integer kitId = kit.getKitId(gender);
				if (kitId == null)
				{
					continue;
				}
				JLabel kitLabel = createKitLabel(kit, kitId);
				kitsList.add(kitLabel, c);
				c.gridy++;
			}
		}
		optionsContainer.add(kitsList, BorderLayout.CENTER);

		optionsContainer.updateUI();
	}

	private JLabel createColorLabel(Colorable colorable)
	{
		JLabel colorLabel = new JLabel();
		colorLabel.setOpaque(true);
		colorLabel.setBackground(colorable.getColor());
		colorLabel.setPreferredSize(COLOR_CHOOSER_SIZE);
		colorLabel.setToolTipText(colorable.getDisplayName());
		colorLabel.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR));
		colorLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!fashionManager.isColorLocked(type))
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					fashionManager.hoverOverColor(type, colorable.getColorId(type));
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				fashionManager.hoverAway();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				fashionManager.hoverSelectColor(type, colorable.getColorId(type));
			}
		});
		return colorLabel;
	}

	private JLabel createKitLabel(Kit kit, Integer kitId)
	{
		JLabel kitLabel = new JLabel();
		kitLabel.setBorder(new EmptyBorder(5, 5, 0, 5));
		kitLabel.setText(kit.getDisplayName());
		kitLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!fashionManager.isKitLocked(slot))
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					fashionManager.hoverOverKit(slot, kitId);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				fashionManager.hoverAway();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				fashionManager.hoverSelectKit(slot, kitId);
			}
		});
		return kitLabel;
	}

	public void closeOptions()
	{
		optionsContainer.removeAll();
		optionsContainer.setVisible(false);
	}

	private void setKitName()
	{
		if (slot == null)
		{
			return;
		}
		String kitName = fashionManager.isNothing(slot) ? "Nothing" : "Not set";
		Integer kitId = fashionManager.virtualKitIdFor(slot);
		if (kitId != null)
		{
			Kit kit = KitUtil.KIT_ID_TO_KIT.get(kitId);
			if (kit != null)
			{
				kitName = kit.getDisplayName();
			}
		}
		label.setText(kitName);
	}

	private void setColorName()
	{
		if (type == null)
		{
			return;
		}
		String colorName = "Not set";
		Integer colorId = fashionManager.virtualColorIdFor(type);
		if (colorId != null && colorMap.containsKey(colorId))
		{
			colorName = colorMap.get(colorId).getDisplayName();
		}
		label.setText(colorName);
	}

	private void setIconColor()
	{
		color = ColorScheme.LIGHT_GRAY_COLOR;
		Integer colorId = fashionManager.virtualColorIdFor(type);
		if (colorId != null)
		{
			Colorable colorable = colorMap.get(colorId);
			if (colorable != null)
			{
				color = colorable.getColor();
			}
		}
		Border border = new RoundedBorder(color, ICON_CORNER_RADIUS);
		icon.setBorder(border);
		updateColorLockButton();
	}

	private void setIconTooltip()
	{
		StringBuilder sb = new StringBuilder();
		if (slot != null)
		{
			sb.append(Text.titleCase(slot));
		}
		else if (type != null)
		{
			sb.append(Text.titleCase(type));
		}

		if (type != null)
		{
			Integer colorId = fashionManager.virtualColorIdFor(type);
			Colorable colorable = colorMap.get(colorId);
			if (colorable != null)
			{
				if (sb.length() > 0)
				{
					sb.append(": ");
				}
				sb.append(colorable.getDisplayName().toLowerCase());
			}
		}
		icon.setToolTipText(sb.toString());
	}

	private void updateLockButton()
	{
		if (slot != null)
		{
			boolean locked = fashionManager.isKitLocked(slot);
			String lockIcon = locked ? "lock" : "unlock";
			lockButton.setIcon(
				new ImageIcon(ImageUtil.loadImageResource(this.getClass(), lockIcon + ".png")));
			String action = locked ? "Unlock" : "Lock";
			lockButton.setToolTipText(action + " " + slot.name().toLowerCase() + " slot");
		}
	}

	private void updateColorLockButton()
	{
		if (type != null)
		{
			boolean locked = fashionManager.isColorLocked(type);
			String lockStr = locked ? "lock" : "unlock";
			BufferedImage lockIcon = ImageUtil.loadImageResource(this.getClass(), lockStr + ".png");
			lockIcon = fillImage(lockIcon, color, 50);
			lockColorButton.setIcon(new ImageIcon(lockIcon));
			String action = locked ? "Unlock" : "Lock";
			lockColorButton.setToolTipText(action + " " + type.name().toLowerCase() + " colour");
		}
	}

	private void updateXButton()
	{
		Integer colorId = fashionManager.virtualColorIdFor(type);
		Integer kitId = fashionManager.virtualKitIdFor(slot);
		xButton.setEnabled(colorId != null || kitId != null);
		String text = slot != null ?
			slot.name().toLowerCase() + " slot" :
			type != null ? type.name().toLowerCase() + " colour" : "";
		xButton.setToolTipText("Clear " + text);
	}

	// copied from ImageUtil, modified to allow for alpha threshold
	public BufferedImage fillImage(final BufferedImage image, final Color color, int alphaThreshold)
	{
		final BufferedImage filledImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < filledImage.getWidth(); x++)
		{
			for (int y = 0; y < filledImage.getHeight(); y++)
			{
				int pixel = image.getRGB(x, y);
				int a = pixel >>> 24;
				if (a < alphaThreshold)
				{
					continue;
				}

				filledImage.setRGB(x, y, color.getRGB());
			}
		}
		return filledImage;
	}
}
