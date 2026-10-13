package eq.uirs.fashionscape.panel.search;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

/**
 * Scrollable list of search result rows, built in batches as the scroll bar nears the bottom
 */
class ResultsList
{
	private static final int BATCH_SIZE = 200;
	// distance from the bottom that starts the next batch
	private static final int LOAD_THRESHOLD_PX = 500;
	// 16px high instead of 7px. long result lists are hard to scroll using the thumb otherwise
	private static final String SCROLL_BAR_STYLE = "minimumThumbSize: 7,16";

	@Getter
	private final JScrollPane scrollPane;
	private final JPanel rows = new JPanel(new GridBagLayout());
	private final GridBagConstraints constraints = new GridBagConstraints();
	private final Function<SearchResult, SearchItemPanel> rowFactory;
	@Getter
	private final List<SearchItemPanel> itemPanels = new ArrayList<>();
	private List<SearchResult> results = Collections.emptyList();
	private boolean layoutPending;

	ResultsList(Function<SearchResult, SearchItemPanel> rowFactory)
	{
		this.rowFactory = rowFactory;

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.gridx = 0;
		constraints.gridy = 0;

		rows.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(rows, BorderLayout.NORTH);

		scrollPane = new JScrollPane(wrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVisible(false);
		scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, SCROLL_BAR_STYLE);
		scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> loadMoreIfNearBottom());
	}

	/**
	 * Replaces the rows with the first batch of results. When keepRows is set, builds at least as many rows as before
	 * so the scroll position holds. Call on the EDT.
	 */
	void show(List<SearchResult> results, boolean keepRows)
	{
		int rowCount = keepRows ? Math.max(BATCH_SIZE, itemPanels.size()) : BATCH_SIZE;
		clear();
		this.results = results;
		addRows(rowCount);
	}

	void clear()
	{
		results = Collections.emptyList();
		itemPanels.clear();
		SwingUtil.fastRemoveAll(rows);
		constraints.gridy = 0;
		rows.revalidate();
		rows.repaint();
	}

	void scrollToTop()
	{
		scrollPane.getVerticalScrollBar().setValue(0);
	}

	// the scroll bar is stale until the last batch is laid out, which only happens while showing
	private void loadMoreIfNearBottom()
	{
		if (!layoutPending && itemPanels.size() < results.size() && scrollPane.isShowing() && isNearBottom())
		{
			addRows(BATCH_SIZE);
		}
	}

	private boolean isNearBottom()
	{
		BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();
		return model.getValue() + model.getExtent() >= model.getMaximum() - LOAD_THRESHOLD_PX;
	}

	private void addRows(int count)
	{
		int end = Math.min(results.size(), itemPanels.size() + count);
		for (int i = itemPanels.size(); i < end; i++)
		{
			SearchItemPanel panel = rowFactory.apply(results.get(i));
			itemPanels.add(panel);
			JPanel marginWrapper = new JPanel(new BorderLayout());
			marginWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
			marginWrapper.setBorder(new EmptyBorder(i == 0 ? 0 : 5, 10, 0, 10));
			marginWrapper.add(panel, BorderLayout.NORTH);
			rows.add(marginWrapper, constraints);
			constraints.gridy++;
		}
		rows.revalidate();
		rows.repaint();
		// runs after the layout from revalidate and re-checks scroll events skipped in the meantime
		layoutPending = true;
		SwingUtilities.invokeLater(() -> {
			layoutPending = false;
			loadMoreIfNearBottom();
		});
	}
}
