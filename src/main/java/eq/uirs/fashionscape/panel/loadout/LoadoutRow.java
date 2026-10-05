package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.SavedLoadout;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.annotation.Nullable;
import javax.swing.JComponent;
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
	private static final int ACTIVE_BAR_WIDTH = 2;

	interface Actions
	{
		void apply(SavedLoadout saved);

		void preview(SavedLoadout saved);

		void endPreview();

		void overwrite(SavedLoadout saved);

		void copy(SavedLoadout saved);

		void rename(SavedLoadout saved, String name);

		void move(SavedLoadout saved, int offset);

		void delete(SavedLoadout saved);
	}

	private final SavedLoadout saved;
	private final Actions actions;

	private final JPanel titleRow = new JPanel(new BorderLayout());
	private final JLabel nameLabel = new JLabel();
	private final FlatTextField nameField = new FlatTextField();
	private boolean renaming;
	private boolean hovered;
	private boolean active;

	LoadoutRow(SavedLoadout saved, @Nullable JComponent summary, JComponent colorBar, boolean first, boolean last,
		Actions actions)
	{
		this.saved = saved;
		this.actions = actions;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		nameLabel.setText(saved.getLoadout().getName());
		nameLabel.setForeground(Color.WHITE);
		// zero width lets long names truncate
		nameLabel.setPreferredSize(new Dimension(0, nameLabel.getPreferredSize().height));
		titleRow.setOpaque(false);
		titleRow.add(nameLabel, BorderLayout.CENTER);
		titleRow.add(createControls(first, last), BorderLayout.EAST);

		// pad the inner panel so the color bar can span full row width
		JPanel content = new JPanel(new BorderLayout());
		content.setOpaque(false);
		content.setBorder(new EmptyBorder(6, 8, 6, 8));
		content.add(titleRow, BorderLayout.NORTH);
		if (summary != null)
		{
			summary.setBorder(new EmptyBorder(4, 0, 0, 0));
			content.add(summary, BorderLayout.CENTER);
		}
		add(content, BorderLayout.CENTER);
		add(colorBar, BorderLayout.SOUTH);

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
		});
		addMouseListener(hoverListener());
	}

	void setActive(boolean active)
	{
		if (this.active != active)
		{
			this.active = active;
			repaint();
		}
	}

	// paints over the children so the bar doesn't take layout space or shift the content
	@Override
	protected void paintChildren(Graphics g)
	{
		super.paintChildren(g);
		if (active)
		{
			g.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			g.fillRect(0, 0, ACTIVE_BAR_WIDTH, getHeight());
		}
	}

	void startRename()
	{
		renaming = true;
		nameField.setText(saved.getLoadout().getName());
		titleRow.remove(nameLabel);
		titleRow.add(nameField, BorderLayout.CENTER);
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
		titleRow.remove(nameField);
		titleRow.add(nameLabel, BorderLayout.CENTER);
		revalidate();
		repaint();
		if (commit && !name.isEmpty() && !name.equals(saved.getLoadout().getName()))
		{
			nameLabel.setText(name);
			actions.rename(saved, name);
		}
	}

	private JPanel createControls(boolean first, boolean last)
	{
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		controls.setOpaque(false);
		controls.add(control("up", "Move up", !first, () -> actions.move(saved, -1)));
		controls.add(control("down", "Move down", !last, () -> actions.move(saved, 1)));
		controls.add(control("x", "Delete", true, () -> actions.delete(saved)));
		return controls;
	}

	private JLabel control(String icon, String tooltip, boolean enabled, Runnable action)
	{
		JLabel label = new JLabel(PanelUtil.icon(icon));
		label.setToolTipText(tooltip);
		label.setEnabled(enabled);
		label.addMouseListener(PanelUtil.hoverCursor(label));
		label.addMouseListener(hoverListener());
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && label.isEnabled())
				{
					action.run();
				}
			}
		});
		return label;
	}

	// shared by the row and its controls, so moving between them keeps the highlight and preview
	private MouseAdapter hoverListener()
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!hovered)
				{
					hovered = true;
					setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
					actions.preview(saved);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (hovered && getMousePosition(true) == null)
				{
					hovered = false;
					setBackground(ColorScheme.DARKER_GRAY_COLOR);
					actions.endPreview();
				}
			}
		};
	}

	private void setUpNameField()
	{
		nameField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameField.setBorder(new EmptyBorder(0, 2, 0, 2));
		nameField.setPreferredSize(new Dimension(0, nameField.getPreferredSize().height));
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
		menu.add(menuItem("Copy to clipboard", () -> actions.copy(saved)));
		return menu;
	}

	private static JMenuItem menuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> action.run());
		return item;
	}
}
