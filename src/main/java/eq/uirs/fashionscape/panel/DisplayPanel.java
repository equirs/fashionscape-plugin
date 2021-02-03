package eq.uirs.fashionscape.panel;

import javax.swing.JPanel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
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
