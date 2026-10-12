package eq.uirs.fashionscape.core.filter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TriState
{
	ANY("Any"),
	YES("Yes"),
	NO("No");

	private final String displayName;

	public boolean matches(boolean value)
	{
		return this == ANY || value == (this == YES);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
