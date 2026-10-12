package eq.uirs.fashionscape.core.filter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Membership
{
	ANY("Any"),
	F2P("F2P"),
	MEMBERS("Members");

	private final String displayName;

	public boolean matches(boolean members)
	{
		return this == ANY || members == (this == MEMBERS);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
