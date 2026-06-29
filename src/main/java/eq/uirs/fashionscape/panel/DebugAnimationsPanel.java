package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.anim.AnimationData;
import eq.uirs.fashionscape.remote.RemoteCategory;
import eq.uirs.fashionscape.remote.RemoteData;
import eq.uirs.fashionscape.remote.RemoteDataHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;

// dev mode tool that allows manually setting idle pose animations, bypassing the plugin
@Slf4j
public class DebugAnimationsPanel extends JPanel
{
	private final Client client;
	private final ClientThread clientThread;

	private final JPanel resultsPanel = new JPanel();

	private boolean hasReceivedAsyncData;

	@Inject
	DebugAnimationsPanel(RemoteDataHandler remote, Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;

		setLayout(new GridLayout(1, 1));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));

		resultsPanel.setLayout(new GridBagLayout());
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

		JPanel resultsWrapper = new JPanel(new BorderLayout());
		resultsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsWrapper.add(resultsPanel, BorderLayout.NORTH);

		scrollPane.setViewportView(resultsWrapper);
		add(scrollPane, BorderLayout.CENTER);

		if (remote.hasSucceeded(RemoteCategory.ANIMATION))
		{
			populateList();
		}
		else
		{
			remote.addOnReceiveDataListener((category, success) -> {
				if (hasReceivedAsyncData)
				{
					return;
				}
				if (category == RemoteCategory.ANIMATION && success)
				{
					hasReceivedAsyncData = true;
					SwingUtilities.invokeLater(this::populateList);
				}
			});
		}
	}

	private void populateList()
	{
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;

		AnimationData defaultAnim = new AnimationData(
			808,
			"default",
			null,
			null,
			null,
			null
		);
		List<AnimationData> allData = new ArrayList<>();
		allData.add(defaultAnim);
		allData.addAll(RemoteData.ANIM_DATA.stream()
			.sorted(Comparator.comparingInt(AnimationData::getAnimId))
			.collect(Collectors.toList()));
		for (AnimationData data : allData)
		{
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
			JLabel label = createLabel(data);
			panel.add(label, BorderLayout.CENTER);

			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(5, 5, 0, 5));
			marginWrapper.add(panel, BorderLayout.NORTH);
			resultsPanel.add(marginWrapper, c);
			c.gridy++;
		}
		revalidate();
		repaint();
	}

	private JLabel createLabel(AnimationData data)
	{
		JLabel label = new JLabel("(" + data.animId + ") " + data.animName);
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(5, 5, 5, 5));
		label.addMouseListener(new MouseAdapter()
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
				clientThread.invokeLater(() -> {
					Player p = client.getLocalPlayer();
					if (p != null)
					{
						p.setIdlePoseAnimation(data.animId);
					}
				});
			}
		});
		return label;
	}
}
