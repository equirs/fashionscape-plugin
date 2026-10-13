package eq.uirs.fashionscape.panel.search;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Stacks equal-height rows by index, leaving space for rows that haven't been added yet
 */
@RequiredArgsConstructor
class FixedRowLayout implements LayoutManager
{
	private static final String INDEX_KEY = "fashionscape.rowIndex";

	private final int rowHeight;
	private final int gap;
	private final int sideMargin;
	@Setter
	private int rowCount;

	static void setIndex(JComponent row, int index)
	{
		row.putClientProperty(INDEX_KEY, index);
	}

	int stride()
	{
		return rowHeight + gap;
	}

	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		return new Dimension(0, Math.max(0, rowCount * stride() - gap));
	}

	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		return preferredLayoutSize(parent);
	}

	@Override
	public void layoutContainer(Container parent)
	{
		int width = Math.max(0, parent.getWidth() - 2 * sideMargin);
		for (Component row : parent.getComponents())
		{
			Object index = ((JComponent) row).getClientProperty(INDEX_KEY);
			if (index instanceof Integer)
			{
				row.setBounds(sideMargin, (Integer) index * stride(), width, rowHeight);
			}
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp)
	{
	}

	@Override
	public void removeLayoutComponent(Component comp)
	{
	}
}
