package eq.uirs.fashionscape.panel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import net.runelite.client.util.ImageUtil;

// for reusing stuff in fashionscape panels
class PanelUtil
{
	// returns image icon `name`.png from panel resources
	static ImageIcon icon(String name)
	{
		return new ImageIcon(ImageUtil.loadImageResource(PanelUtil.class, name + ".png"));
	}

	// show hand cursor while hovering, default cursor otherwise
	static MouseAdapter hoverCursor(Component target)
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (e.getComponent().isEnabled())
				{
					target.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				target.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		};
	}

	// sets icon and tooltip on a toggle button
	static void applyLockButton(JButton button, boolean locked, String target)
	{
		button.setIcon(icon(locked ? "lock" : "unlock"));
		button.setToolTipText((locked ? "Unlock " : "Lock ") + target);
	}
}
