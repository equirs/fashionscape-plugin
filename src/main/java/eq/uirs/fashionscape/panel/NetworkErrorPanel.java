package eq.uirs.fashionscape.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class NetworkErrorPanel extends JPanel
{
	private static final Color BACKGROUND_COLOR = new Color(90, 0, 0);

	NetworkErrorPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(5, 10, 10, 10));
		setBackground(BACKGROUND_COLOR);

		JLabel text = new JLabel("<html><body>Failed to fetch core plugin data.<br>Restart the client to try again.</body></html>");
		add(text, BorderLayout.CENTER);
	}
}
