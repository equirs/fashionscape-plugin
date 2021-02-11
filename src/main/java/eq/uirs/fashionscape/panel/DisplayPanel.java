package eq.uirs.fashionscape.panel;

import javax.swing.JPanel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DisplayPanel extends JPanel
{
	boolean shouldClearSearch = true;
	private final FashionscapeSearchPanel searchPanel;

	@Override
	public void removeAll()
	{
		// TODO switching tabs without clearing results = shit performance. feels bad to do this
		if (shouldClearSearch)
		{
			searchPanel.clearResults();
		}
		super.removeAll();
	}
}
