package eq.uirs.fashionscape.panel;

import com.google.common.base.Objects;
import eq.uirs.fashionscape.data.kit.JawIcon;
import eq.uirs.fashionscape.swap.SwapManager;
import eq.uirs.fashionscape.swap.event.IconChangedListener;
import eq.uirs.fashionscape.swap.event.IconLockChangedListener;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Special base panel handling BA/SW icons since they're tied to jaw kits
 */
@Slf4j
public class JawIconPanel extends DropdownIconPanel
{
	private final SwapManager swapManager;
	private final ItemManager itemManager;
	private final KitColorOpener kitColorOpener;
	private JawIcon jawIcon = null;

	JawIconPanel(BufferedImage image, ClientThread clientThread, SwapManager swapManager, ItemManager itemManager,
				 KitColorOpener kitColorOpener, Integer iconId)
	{
		super(image, clientThread);
		this.swapManager = swapManager;
		this.itemManager = itemManager;
		this.kitColorOpener = kitColorOpener;
		Arrays.stream(JawIcon.values())
			.filter(i -> Objects.equal(i.getId(), iconId))
			.findFirst()
			.ifPresent(i -> this.jawIcon = i);

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);

		setIconTooltip();

		JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		setIcon(icon, image);
		icon.setBorder(new RoundedBorder(ColorScheme.LIGHT_GRAY_COLOR, ICON_CORNER_RADIUS));

		swapManager.addEventListener(new IconChangedListener(e -> {
			if (!Objects.equal(jawIcon, e.getIcon()))
			{
				this.jawIcon = e.getIcon();
				setIconName();
				updateIconImage();
				updateXButton();
			}
		}));
		swapManager.addEventListener(new IconLockChangedListener(e -> updateLockButton()));
		configureButton(lockButton);
		lockButton.addActionListener(e -> {
			swapManager.toggleIconLocked();
			updateLockButton();
			updateXButton();
		});
		buttons.add(lockButton);

		configureButton(xButton);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(() -> {
			swapManager.revertIcon();
			SwingUtilities.invokeLater(() -> {
				updateLockButton();
				updateXButton();
			});
		}));
		buttons.add(xButton);

		updateLockButton();
		updateXButton();

		optionsContainer.setLayout(new BorderLayout());
		optionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		optionsContainer.setVisible(false);
		add(optionsContainer, BorderLayout.SOUTH);
		rightPanel.add(buttons, BorderLayout.EAST);
		add(rightPanel, BorderLayout.CENTER);

		setIconName();
		updateIconImage();
	}

	@Override
	void openDropdown()
	{
		kitColorOpener.openOptions(null, null);
	}

	public void openOptions()
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

		JPanel iconsList = new JPanel();
		iconsList.setLayout(new GridBagLayout());
		iconsList.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		iconsList.setBorder(new EmptyBorder(10, 0, 0, 0));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;

		for (JawIcon jawIcon : JawIcon.values())
		{
			if (jawIcon == JawIcon.NOTHING)
			{
				continue;
			}
			JLabel iconLabel = new JLabel();
			iconLabel.setBorder(new EmptyBorder(5, 5, 0, 5));
			iconLabel.setText(jawIcon.getDisplayName());
			iconLabel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					if (!swapManager.isIconLocked())
					{
						setCursor(new Cursor(Cursor.HAND_CURSOR));
					}
					swapManager.hoverOverIcon(jawIcon);
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
					swapManager.hoverSelectIcon(jawIcon);
				}
			});
			iconsList.add(iconLabel, c);
			c.gridy++;
		}
		optionsContainer.add(iconsList, BorderLayout.CENTER);
		optionsContainer.updateUI();
	}

	public void closeOptions()
	{
		optionsContainer.removeAll();
		optionsContainer.setVisible(false);
	}

	private void setIconName()
	{
		String iconName = "Not set";
		if (jawIcon != null)
		{
			iconName = jawIcon.getDisplayName();
		}
		label.setText(iconName);
	}

	private void updateIconImage()
	{
		if (jawIcon != null && jawIcon != JawIcon.NOTHING)
		{
			clientThread.invokeLater(() -> {
				ItemComposition itemComposition = itemManager.getItemComposition(jawIcon.getId());
				AsyncBufferedImage image = itemManager.getImage(itemComposition.getId());
				image.addTo(icon);
			});
		}
		else
		{
			BufferedImage image = ImageUtil.loadImageResource(getClass(), "icon.png");
			setIcon(icon, image);
		}
	}

	private void updateXButton()
	{
		xButton.setEnabled(jawIcon != null);
		xButton.setToolTipText("Clear icon");
	}

	private void updateLockButton()
	{
		boolean locked = swapManager.isIconLocked();
		String lockIcon = locked ? "lock" : "unlock";
		lockButton.setIcon(
			new ImageIcon(ImageUtil.loadImageResource(this.getClass(), lockIcon + ".png")));
		String action = locked ? "Unlock" : "Lock";
		lockButton.setToolTipText(action + " icon");
	}

	private void setIconTooltip()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Icon");
		if (jawIcon != null)
		{
			if (sb.length() > 0)
			{
				sb.append(": ");
			}
			sb.append(jawIcon.getDisplayName().toLowerCase());
		}
		icon.setToolTipText(sb.toString());
	}
}
