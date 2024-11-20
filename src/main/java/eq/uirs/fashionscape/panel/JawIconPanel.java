package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.IconChangedListener;
import eq.uirs.fashionscape.core.event.IconLockChangedListener;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	private final FashionManager fashionManager;
	private final ItemManager itemManager;
	private final KitColorOpener kitColorOpener;

	private final IconChangedListener iconChangedListener;
	private final IconLockChangedListener iconLockChangedListener;

	JawIconPanel(BufferedImage image, ClientThread clientThread, FashionManager fashionManager, ItemManager itemManager,
				 KitColorOpener kitColorOpener)
	{
		super(image, clientThread);
		this.fashionManager = fashionManager;
		this.itemManager = itemManager;
		this.kitColorOpener = kitColorOpener;

		this.iconChangedListener = new IconChangedListener(e -> {
			if (e.getModelType() == ModelType.VIRTUAL)
			{
				refreshData();
			}
		});
		this.iconLockChangedListener = new IconLockChangedListener(e -> updateLockButton());

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(nonHighlightColor);
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		highlightPanels.add(rightPanel);
		rightPanel.add(label, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		setIcon(icon, image);
		icon.setBorder(new RoundedBorder(ColorScheme.LIGHT_GRAY_COLOR, ICON_CORNER_RADIUS));

		configureButton(lockButton);
		lockButton.addActionListener(e -> fashionManager.toggleIconLocked());
		buttons.add(lockButton);

		configureButton(xButton);
		xButton.setIcon(new ImageIcon(ImageUtil.loadImageResource(this.getClass(), "x.png")));
		xButton.addActionListener(e -> clientThread.invokeLater(fashionManager::revertIcon));
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

		Events.addListener(iconChangedListener);
		Events.addListener(iconLockChangedListener);
	}

	@Override
	void removeListeners()
	{
		Events.removeListener(iconChangedListener);
		Events.removeListener(iconLockChangedListener);
	}

	private void refreshData()
	{
		setIconName();
		updateIconImage();
		setIconTooltip();
		updateLockButton();
		updateXButton();
	}

	@Override
	void openDropdown(MouseEvent e)
	{
		kitColorOpener.openOptions(null, null);
	}

	@Override
	void tryClear()
	{
		// clear the icon just as pressing the "x" button would
		clientThread.invokeLater(fashionManager::revertIcon);
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
			JLabel iconLabel = createIconLabel(jawIcon);
			iconsList.add(iconLabel, c);
			c.gridy++;
		}
		optionsContainer.add(iconsList, BorderLayout.CENTER);
		optionsContainer.updateUI();
	}

	private JLabel createIconLabel(JawIcon jawIcon)
	{
		JLabel iconLabel = new JLabel();
		iconLabel.setBorder(new EmptyBorder(5, 5, 0, 5));
		iconLabel.setText(jawIcon.getDisplayName());
		iconLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!fashionManager.isIconLocked())
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					fashionManager.hoverOverIcon(jawIcon);
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
				fashionManager.hoverSelectIcon(jawIcon);
			}
		});
		return iconLabel;
	}

	public void closeOptions()
	{
		optionsContainer.removeAll();
		optionsContainer.setVisible(false);
	}

	private void setIconName()
	{
		String iconName = "Not set";
		JawIcon jawIcon = fashionManager.virtualIcon();
		if (jawIcon != null)
		{
			iconName = jawIcon.getDisplayName();
		}
		label.setText(iconName);
	}

	private void updateIconImage()
	{
		JawIcon jawIcon = fashionManager.virtualIcon();
		if (jawIcon != null && jawIcon != JawIcon.NOTHING)
		{
			clientThread.invokeLater(() -> {
				try
				{
					ItemComposition itemComposition = itemManager.getItemComposition(jawIcon.getId());
					AsyncBufferedImage image = itemManager.getImage(itemComposition.getId());
					image.addTo(icon);
				}
				catch (Exception e)
				{
					return false;
				}
				return true;
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
		JawIcon jawIcon = fashionManager.virtualIcon();
		xButton.setEnabled(jawIcon != null);
		xButton.setToolTipText("Clear icon");
	}

	private void updateLockButton()
	{
		boolean locked = fashionManager.isIconLocked();
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
		JawIcon jawIcon = fashionManager.virtualIcon();
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
