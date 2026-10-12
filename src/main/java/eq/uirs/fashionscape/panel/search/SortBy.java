package eq.uirs.fashionscape.panel.search;

public enum SortBy
{
	COLOR_MATCH,
	ALPHABETICAL,
	GE_PRICE,
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
			case GE_PRICE:
				return "GE price";
			default:
				return "";
		}
	}
}
