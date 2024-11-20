package eq.uirs.fashionscape.panel;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;

/**
 * Panel that opens a dropdown menu of options to select
 */
@Slf4j
abstract class DropdownIconPanel extends AbsIconLabelPanel
{
	static final Dimension BUTTON_SIZE = new Dimension(16, 16);
	static final int ICON_CORNER_RADIUS = 15;

	protected final JButton lockButton = new JButton();
	protected final JButton xButton = new JButton();
	// constrain items in the list of dropdown results
	protected final JPanel optionsContainer = new JPanel();

	DropdownIconPanel(BufferedImage image, ClientThread clientThread)
	{
		super(image, clientThread);

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
				if (SwingUtilities.isRightMouseButton(e))
				{
					tryClear();
				}
				else
				{
					openDropdown(e);
				}
			}
		});
	}

	@Override
	public void addNotify()
	{
		addListeners();
		super.addNotify();
	}

	@Override
	public void removeNotify()
	{
		removeListeners();
		super.removeNotify();
	}

	protected void configureButton(JButton button)
	{
		button.setBorder(new EmptyBorder(0, 2, 0, 2));
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setPreferredSize(BUTTON_SIZE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
	}

	abstract void addListeners();

	abstract void removeListeners();

	abstract void openDropdown(MouseEvent e);

	abstract void tryClear();
}
