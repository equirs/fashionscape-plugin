package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@Getter
public enum LegsKit implements Kit
{
	PLAIN_L(36, 70),
	SHORTS(37, 274),
	FLARES(38, 72),
	TURN_UPS(39, 76),
	TATTY_L(40, 75),
	BEACH(41, 275),
	REGAL_L(100, 276),
	LEGGINGS(101, 277),
	SIDE_STRIPES(102, 278),
	RIPPED_L(103, 279),
	PATCHED(104, 280),
	SKIRT(281, 71),
	LONG_SKIRT(282, 73),
	LONG_NARROW_SKIRT(283, 74),
	SHORT_SKIRT(284, 77),
	LAYERED_L(285, 78),
	SASH_AND_DOTS(286, 135),
	BIG_HEM(287, 136),
	SASH_AND_TROUSERS(288, 137),
	PATTERNED(289, 138),
	TORN_SKIRT(290, 139),
	PATCHED_SKIRT(291, 140);

	private final Integer mascKitId;
	private final Integer femKitId;

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
			case TATTY_L:
				return "Tatty";
			case REGAL_L:
				return "Regal";
			case RIPPED_L:
				return "Ripped";
			case LAYERED_L:
				return "Layered";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
