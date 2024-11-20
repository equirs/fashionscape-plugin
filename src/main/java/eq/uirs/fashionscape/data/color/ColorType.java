package eq.uirs.fashionscape.data.color;

import net.runelite.client.util.Text;

public enum ColorType
{
	HAIR,
	TORSO,
	LEGS,
	BOOTS,
	SKIN;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}

	public Colorable[] getColorables()
	{
		switch (this)
		{
			case HAIR:
				return HairColor.values();
			case TORSO:
			case LEGS:
				return ClothingColor.values();
			case BOOTS:
				return BootsColor.values();
			case SKIN:
				return SkinColor.values();
		}
		return new Colorable[0];
	}
}
