package eq.uirs.fashionscape.data;

import net.runelite.client.util.Text;

public enum ColorType
{
	HAIR,
	TORSO,
	LEGS,
	FEET,
	SKIN;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}
}
