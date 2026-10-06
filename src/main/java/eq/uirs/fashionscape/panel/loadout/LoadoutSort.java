package eq.uirs.fashionscape.panel.loadout;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LoadoutSort
{
	CUSTOM("Custom"),
	NAME("Name");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
