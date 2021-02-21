package eq.uirs.fashionscape.panel.search;

public enum SortBy
{
	COLOR_MATCH,
	ALPHABETICAL,
	RELEASE;

	@Override
	public String toString()
	{
		switch (this)
		{
			case RELEASE:
				return "Release";
			case ALPHABETICAL:
				return "Alphabetical";
			case COLOR_MATCH:
				return "Colour match";
			default:
				return "";
		}
	}
}
