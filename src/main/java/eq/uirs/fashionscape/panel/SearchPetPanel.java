package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.Pet;
import eq.uirs.fashionscape.swap.SwapManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;

public class SearchPetPanel extends AbsIconLabelPanel
{
	final Integer petId;
	private final SwapManager swapManager;
	private final Pet pet;

	// TODO need pet changing listener
	SearchPetPanel(BufferedImage image, ClientThread clientThread, SwapManager swapManager, Pet pet, Double score,
				   SelectionChangingListener listener)
	{
		super(image, clientThread);
		this.pet = pet;
		this.petId = pet.getNpcId();
		this.swapManager = swapManager;

		MouseAdapter panelMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!swapManager.isPetLocked())
				{
					for (JPanel panel : highlightPanels)
					{
						matchComponentBackground(panel, ColorScheme.DARK_GRAY_HOVER_COLOR);
					}
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				swapManager.hoverOverPet(pet.getNpcId());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, defaultBackgroundColor());
				}
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				swapManager.hoverAway();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				// pre-emptively set background
				boolean isAlreadySelected = Objects.equals(swapManager.swappedPetId(), pet.getNpcId());
				Color bg = isAlreadySelected ? nonHighlightColor : ColorScheme.MEDIUM_GRAY_COLOR;
				for (JPanel panel : highlightPanels)
				{
					matchComponentBackground(panel, bg);
				}
				// now swap it
				clientThread.invokeLater(() -> {
					if (!swapManager.isPetLocked())
					{
						listener.petChanging();
						swapManager.hoverSelectPet(pet.getNpcId());
					}
				});
			}
		};

		addMouseListener(panelMouseListener);

		// Item details panel
		int rows = score == null ? 1 : 2;
		JPanel rightPanel = new JPanel(new GridLayout(rows, 1));
		rightPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
		highlightPanels.add(rightPanel);
		String petName = pet.getDisplayName();
		label.setText(petName);
		rightPanel.add(label);

		if (score != null)
		{
			DecimalFormat format = new DecimalFormat("#.#");
			JLabel scoreLabel = new JLabel();
			scoreLabel.setForeground(Color.WHITE);
			scoreLabel.setMaximumSize(new Dimension(0, 0));
			scoreLabel.setPreferredSize(new Dimension(0, 0));
			scoreLabel.setText(format.format(score * 100.0) + "%");
			scoreLabel.setForeground(getScoreColor(score));
			rightPanel.add(scoreLabel);
		}

		for (JPanel panel : highlightPanels)
		{
			matchComponentBackground(panel, defaultBackgroundColor());
		}

		add(rightPanel, BorderLayout.CENTER);
	}

	private Color defaultBackgroundColor()
	{
		if (Objects.equals(swapManager.swappedPetId(), pet.getNpcId()))
		{
			return ColorScheme.MEDIUM_GRAY_COLOR;
		}
		else
		{
			return nonHighlightColor;
		}
	}
}
