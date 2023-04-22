package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum LegsKit implements Kit
{
	PLAIN_L(36, 70),
	SHORTS(37, null),
	FLARES(38, 72),
	TURN_UPS(39, 76),
	TATTY_L(40, 75),
	BEACH(41, null),
	PRINCELY_L(100, null),
	LEGGINGS(101, null),
	SIDE_STRIPES(102, null),
	RIPPED_L(103, null),
	PATCHED(104, null),
	SKIRT(null, 71),
	LONG_SKIRT(null, 73),
	LONG_NARROW_SKIRT(null, 74),
	SHORT_SKIRT(null, 77),
	LAYERED_L(null, 78),
	SASH_AND_DOTS(null, 135),
	BIG_HEM(null, 136),
	SASH_AND_TROUSERS(null, 137),
	PATTERNED(null, 138),
	TORN_SKIRT(null, 139),
	PATCHED_SKIRT(null, 140);

	private final Integer maleKitId;

	private final Integer femaleKitId;

	@Override
	public KitType getKitType()
	{
		return KitType.LEGS;
	}

	@Override
	public String getDisplayName()
	{
		switch (this)
		{
			case PLAIN_L:
				return "Plain";
			case TURN_UPS:
				return "Turn-ups";
			case TATTY_L:
				return "Tatty";
			case PRINCELY_L:
				return "Princely";
			case SIDE_STRIPES:
				return "Side-stripes";
			case RIPPED_L:
				return "Ripped";
			case LAYERED_L:
				return "Layered";
			case SASH_AND_DOTS:
				return "Sash & dots";
			case SASH_AND_TROUSERS:
				return "Sash & trousers";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}

	@Override
	public Integer getKitId(boolean isFemale)
	{
		return isFemale ? femaleKitId : maleKitId;
	}
}
