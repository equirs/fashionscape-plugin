package eq.uirs.fashionscape.panel;

import com.google.common.base.Objects;
import eq.uirs.fashionscape.data.ColorType;
import eq.uirs.fashionscape.data.Colorable;
import eq.uirs.fashionscape.data.Kit;
import eq.uirs.fashionscape.swap.ColorChangedListener;
import eq.uirs.fashionscape.swap.ColorLockChangedListener;
import eq.uirs.fashionscape.swap.KitChangedListener;
import eq.uirs.fashionscape.swap.LockChanged;
import eq.uirs.fashionscape.swap.LockChangedListener;
import eq.uirs.fashionscape.swap.SwapManager;
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
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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
public class KitItemPanel extends AbsIconLabelPanel
{
	private static final Dimension COLOR_CHOOSER_SIZE = new Dimension(15, 15);
	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final Dimension BUTTON_SIZE = new Dimension(16, 16);
	private static final int ICON_CORNER_RADIUS = 15;

	@Getter
	private final ColorType type;
	@Getter
	private final KitType slot;

	private final SwapManager swapManager;

	private final Map<Integer, Colorable> colorMap = new HashMap<>();

	private final JButton lockKitButton = new JButton();
	private final JButton lockColorButton = new JButton();
	private final JButton xButton = new JButton();
	// constrain items in the list of dropdown results
	private final JPanel optionsContainer = new JPanel();
	private final List<Kit> allKits = new ArrayList<>();

	private Integer colorId;
	private Color color;
	private Integer kitId;

	public KitItemPanel(SwapManager swapManager, ColorType type, Integer colorId, KitType slot, Integer kitId,
						KitColorOpener kitColorOpener, BufferedImage image, ClientThread clientThread)
	{
		super(image, clientThread);
		this.swapManager = swapManager;
		this.type = type;
		this.slot = slot;
		this.colorId = colorId;
		this.kitId = kitId;

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);

		icon.setPreferredSize(ICON_SIZE);
		setIconTooltip();
		icon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				kitColorOpener.openOptions(slot, type);
			}
		});

		int cols = type != null && slot != null ? 3 : 2;
		JPanel buttons = new JPanel(new GridLayout(1, cols, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (slot != null)
		{
			allKits.clear();
			Arrays.stream(Kit.values())
				.filter(kit -> kit.getKitType().equals(slot))
				.forEach(allKits::add);
			setKitName();
			setIcon(icon, image);
			swapManager.addEventListener(new KitChangedListener(e -> {
				if (Objects.equal(slot, e.getSlot()))
				{
					this.kitId = e.getKitId();
					setKitName();
					updateXButton();
				}
			}));
			lockKitButton.setBorder(new EmptyBorder(0, 2, 0, 2));
			lockKitButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			lockKitButton.setPreferredSize(BUTTON_SIZE);
			lockKitButton.setFocusPainted(false);
			lockKitButton.setBorderPainted(false);
			lockKitButton.setContentAreaFilled(false);
			lockKitButton.addActionListener(e -> {
				swapManager.toggleKitLocked(slot);
				updateKitLockButton();
				updateXButton();
			});
			buttons.add(lockKitButton);
		}
		else if (type != null)
		{
			BufferedImage colorImage = ImageUtil.loadImageResource(this.getClass(), "color.png");
			setIcon(icon, colorImage);
			setColorName();
		}

		if (type != null)
		{
			for (Colorable colorable : type.getColorables())
			{
				colorMap.put(colorable.getColorId(type), colorable);
			}
			setIconColor();
			swapManager.addEventListener(new ColorChangedListener(e -> {
				if (Objects.equal(type, e.getType()))
				{
					this.colorId = e.getColorId();
					setIconColor();
					if (slot == null)
					{
						setColorName();
					}
					setIconTooltip();
					updateXButton();
				}
			}));
			lockColorButton.setBorder(new EmptyBorder(0, 2, 0, 2));
			lockColorButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			lockColorButton.setPreferredSize(BUTTON_SIZE);
			lockColorButton.setFocusPainted(false);
			lockColorButton.setBorderPainted(false);
			lockColorButton.setContentAreaFilled(false);
			lockColorButton.addActionListener(e -> {
				swapManager.toggleColorLocked(type);
				updateColorLockButton();
				updateXButton();
			});
			buttons.add(lockColorButton);
		}
		else
		{
			icon.setBorder(new RoundedBorder(ColorScheme.LIGHT_GRAY_COLOR, ICON_CORNER_RADIUS));
		}

		xButton.setPreferredSize(BUTTON_SIZE);
		xButton.setBorder(new EmptyBorder(0, 2, 0, 2));
		xButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		xButton.setFocusPainted(false);
		xButton.setBorderPainted(false);
		xButton.setContentAreaFilled(false);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revert(slot, type);
			SwingUtilities.invokeLater(() -> {
				updateKitLockButton();
				updateColorLockButton();
				updateXButton();
			});
		}));
		buttons.add(xButton);

		updateKitLockButton();
		updateColorLockButton();
		updateXButton();

		optionsContainer.setLayout(new BorderLayout());
		optionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		optionsContainer.setVisible(false);
		add(optionsContainer, BorderLayout.SOUTH);
		rightPanel.add(buttons, BorderLayout.EAST);
		add(rightPanel, BorderLayout.CENTER);

		swapManager.addEventListener(new LockChangedListener(e -> {
			if (Objects.equal(e.getSlot(), slot) && e.getType() != LockChanged.Type.ITEM)
			{
				updateKitLockButton();
			}
		}));
		swapManager.addEventListener(new ColorLockChangedListener(e -> {
			if (Objects.equal(e.getType(), type))
			{
				updateColorLockButton();
			}
		}));
	}

	public void openOptions(boolean isFemale)
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
						if (!swapManager.isColorLocked(type))
						{
							setCursor(new Cursor(Cursor.HAND_CURSOR));
						}
						swapManager.hoverOverColor(type, colorable.getColorId(type));
					}

					@Override
					public void mouseExited(MouseEvent e)
					{
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
						swapManager.hoverAway();
					}

					@Override
					public void mouseReleased(MouseEvent e)
					{
						swapManager.hoverSelectColor(type, colorable.getColorId(type));
					}
				});

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
				if (kit.isFemale() != isFemale)
				{
					continue;
				}
				JLabel kitLabel = new JLabel();
				kitLabel.setBorder(new EmptyBorder(5, 5, 0, 5));
				kitLabel.setText(kit.getDisplayName());
				kitLabel.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseEntered(MouseEvent e)
					{
						if (!swapManager.isKitLocked(slot))
						{
							setCursor(new Cursor(Cursor.HAND_CURSOR));
						}
						swapManager.hoverOverKit(slot, kit.getKitId());
					}

					@Override
					public void mouseExited(MouseEvent e)
					{
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
						swapManager.hoverAway();
					}

					@Override
					public void mouseReleased(MouseEvent e)
					{
						swapManager.hoverSelectKit(slot, kit.getKitId());
					}
				});
				kitsList.add(kitLabel, c);
				c.gridy++;
			}
		}
		optionsContainer.add(kitsList, BorderLayout.CENTER);

		optionsContainer.updateUI();
	}

	public void closeOptions()
	{
		optionsContainer.removeAll();
		optionsContainer.setVisible(false);
	}

	private void setKitName()
	{
		String kitName = "Not set";
		if (kitId != null)
		{
			Kit kit = SwapManager.KIT_MAP.get(kitId);
			if (kit != null)
			{
				kitName = kit.getDisplayName();
			}
		}
		label.setText(kitName);
	}

	private void setColorName()
	{
		String colorName = "Not set";
		if (colorId != null)
		{
			try
			{
				colorName = colorMap.get(colorId).getDisplayName();
			}
			catch (Exception e)
			{
				log.info("color id {}", colorId);
			}
		}
		label.setText(colorName);
	}

	private void setIconColor()
	{
		color = ColorScheme.LIGHT_GRAY_COLOR;
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

	private void updateKitLockButton()
	{
		if (slot != null)
		{
			boolean locked = swapManager.isKitLocked(slot);
			String lockIcon = locked ? "lock" : "unlock";
			lockKitButton.setIcon(
				new ImageIcon(ImageUtil.loadImageResource(this.getClass(), lockIcon + ".png")));
			String action = locked ? "Unlock" : "Lock";
			lockKitButton.setToolTipText(action + " " + slot.name().toLowerCase() + " slot");
		}
	}

	private void updateColorLockButton()
	{
		if (type != null)
		{
			boolean locked = swapManager.isColorLocked(type);
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
