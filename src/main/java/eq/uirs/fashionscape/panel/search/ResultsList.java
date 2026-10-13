package eq.uirs.fashionscape.panel.search;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

/**
 * Scrollable list of search result rows. Space is reserved for every result, but rows are only built as they
 * scroll into view.
 */
class ResultsList
{
	// SearchItemPanel's height: 32px icon + 5px padding each side
	private static final int ROW_HEIGHT = 42;
	private static final int ROW_GAP = 5;
	private static final int SIDE_MARGIN = 10;
	// builds rows this far outside the visible area, so they're ready before they scroll in
	private static final int BUFFER_PX = 500;
	// 16px high instead of 7px. long result lists are hard to scroll using the thumb with the default
	private static final String SCROLL_BAR_STYLE = "minimumThumbSize: 7,16";

	@Getter
	private final JScrollPane scrollPane;
	private final FixedRowLayout layout = new FixedRowLayout(ROW_HEIGHT, ROW_GAP, SIDE_MARGIN);
	private final JPanel rows = new JPanel(layout);
	private final Function<SearchResult, SearchItemPanel> rowFactory;
	@Getter
	private final List<SearchItemPanel> itemPanels = new ArrayList<>();
	private List<SearchResult> results = Collections.emptyList();
	// built rows by result index, null until scrolled into view
	private SearchItemPanel[] builtRows = new SearchItemPanel[0];

	ResultsList(Function<SearchResult, SearchItemPanel> rowFactory)
	{
		this.rowFactory = rowFactory;

		rows.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(rows, BorderLayout.NORTH);

		scrollPane = new JScrollPane(wrapper);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setVisible(false);
		scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, SCROLL_BAR_STYLE);
		// fires on scroll and when the list or viewport changes size
		scrollPane.getViewport().addChangeListener(e -> buildVisibleRows());
	}

	/**
	 * Replaces results, keeps scroll position. Call on the EDT.
	 */
	void show(List<SearchResult> results)
	{
		clear();
		this.results = results;
		builtRows = new SearchItemPanel[results.size()];
		layout.setRowCount(results.size());
		rows.revalidate();
		buildVisibleRows();
	}

	void clear()
	{
		results = Collections.emptyList();
		builtRows = new SearchItemPanel[0];
		itemPanels.clear();
		layout.setRowCount(0);
		SwingUtil.fastRemoveAll(rows);
		rows.revalidate();
		rows.repaint();
	}

	void scrollToTop()
	{
		scrollPane.getVerticalScrollBar().setValue(0);
	}

	private void buildVisibleRows()
	{
		Rectangle view = scrollPane.getViewport().getViewRect();
		int first = Math.max(0, (view.y - BUFFER_PX) / layout.stride());
		int last = Math.min(results.size() - 1, (view.y + view.height + BUFFER_PX) / layout.stride());
		boolean added = false;
		for (int i = first; i <= last; i++)
		{
			if (builtRows[i] == null)
			{
				SearchItemPanel row = rowFactory.apply(results.get(i));
				FixedRowLayout.setIndex(row, i);
				builtRows[i] = row;
				itemPanels.add(row);
				rows.add(row);
				added = true;
			}
		}
		if (added)
		{
			// lay out new rows now so they don't flash in blank. list height won't change
			rows.validate();
			rows.repaint();
		}
	}
}
