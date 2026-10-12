package eq.uirs.fashionscape.core.filter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ValueSource
{
	ANY("Any"),
	GE("GE price"),
	ALCH("High alch");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
