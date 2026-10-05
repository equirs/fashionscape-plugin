package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.SavedLoadout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;

class LoadoutRow extends JPanel
{
	interface Actions
	{
		void apply(SavedLoadout saved);

		void overwrite(SavedLoadout saved);

		void rename(SavedLoadout saved, String name);

		void delete(SavedLoadout saved);
	}

	private final SavedLoadout saved;
	private final Actions actions;

	private final JLabel nameLabel = new JLabel();
	private final FlatTextField nameField = new FlatTextField();
	private boolean renaming;

	LoadoutRow(SavedLoadout saved, Actions actions)
	{
		this.saved = saved;
		this.actions = actions;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(6, 8, 6, 8));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		nameLabel.setText(saved.getLoadout().getName());
		nameLabel.setForeground(Color.WHITE);
		add(nameLabel, BorderLayout.NORTH);

		setUpNameField();
		setComponentPopupMenu(createMenu());
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !renaming)
				{
					actions.apply(saved);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	void startRename()
	{
		renaming = true;
		nameField.setText(saved.getLoadout().getName());
		remove(nameLabel);
		add(nameField, BorderLayout.NORTH);
		revalidate();
		repaint();
		nameField.getTextField().requestFocusInWindow();
		nameField.getTextField().selectAll();
	}

	private void finishRename(boolean commit)
	{
		if (!renaming)
		{
			return;
		}
		renaming = false;
		String name = nameField.getText().trim();
		remove(nameField);
		add(nameLabel, BorderLayout.NORTH);
		revalidate();
		repaint();
		if (commit && !name.isEmpty() && !name.equals(saved.getLoadout().getName()))
		{
			nameLabel.setText(name);
			actions.rename(saved, name);
		}
	}

	private void setUpNameField()
	{
		nameField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameField.setBorder(new EmptyBorder(0, 2, 0, 2));
		nameField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					finishRename(true);
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					finishRename(false);
				}
			}
		});
		nameField.getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				finishRename(true);
			}
		});
	}

	private JPopupMenu createMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Update with current look", () -> actions.overwrite(saved)));
		menu.add(menuItem("Rename", this::startRename));
		menu.addSeparator();
		menu.add(menuItem("Delete", () -> actions.delete(saved)));
		return menu;
	}

	private static JMenuItem menuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> action.run());
		return item;
	}
}
