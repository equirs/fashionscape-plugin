package eq.uirs.fashionscape.panel;

public enum SortBy
{
	COLOR_MATCH,
	ALPHABETICAL,
	ITEM_ID;

	@Override
	public String toString()
	{
		switch (this)
		{
			case ITEM_ID:
				return "Default";
			case ALPHABETICAL:
				return "Alphabetical";
			case COLOR_MATCH:
				return "Colour match";
			default:
				return "";
		}
	}
}
