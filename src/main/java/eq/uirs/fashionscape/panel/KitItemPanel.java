package eq.uirs.fashionscape.panel;

import com.google.common.base.Objects;
import eq.uirs.fashionscape.data.Kit;
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
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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
	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final Dimension BUTTON_SIZE = new Dimension(16, 16);

	@Getter
	private final KitType slot;

	private final SwapManager swapManager;

	private final JButton lockKitButton = new JButton();
	private final JButton xButton = new JButton();
	// constrain items in the list of dropdown results
	private final JPanel optionsContainer = new JPanel();
	private final List<Kit> allKits = new ArrayList<>();

	private Integer kitId;

	public KitItemPanel(SwapManager swapManager, KitType slot, Integer kitId, KitColorOpener kitColorOpener,
						BufferedImage image, ClientThread clientThread)
	{
		super(image, clientThread);
		this.swapManager = swapManager;
		this.slot = slot;
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
				kitColorOpener.openOptions(slot);
			}
		});

		JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
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

		xButton.setPreferredSize(BUTTON_SIZE);
		xButton.setBorder(new EmptyBorder(0, 2, 0, 2));
		xButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		xButton.setFocusPainted(false);
		xButton.setBorderPainted(false);
		xButton.setContentAreaFilled(false);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revert(slot);
			SwingUtilities.invokeLater(() -> {
				updateKitLockButton();
				updateXButton();
			});
		}));
		buttons.add(xButton);

		updateKitLockButton();
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

	private void setIconTooltip()
	{
		StringBuilder sb = new StringBuilder();
		if (slot != null)
		{
			sb.append(Text.titleCase(slot));
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

	private void updateXButton()
	{
		xButton.setEnabled(kitId != null);
		String text = slot != null ?
			slot.name().toLowerCase() + " slot" :
			"slot";
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
