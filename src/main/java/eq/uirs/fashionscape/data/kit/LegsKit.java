package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum LegsKit implements Kit
{
	PLAIN_L("Plain", false, 36),
	SHORTS("Shorts", false, 37),
	FLARES("Flares", false, 38),
	TURN_UPS("Turn-ups", false, 39),
	TATTY_L("Tatty", false, 40),
	BEACH("Beach", false, 41),
	PRINCELY_L("Princely", false, 100),
	LEGGINGS("Leggings", false, 101),
	SIDE_STRIPES("Side-stripes", false, 102),
	RIPPED_L("Ripped", false, 103),
	PATCHED("Patched", false, 104),

	PLAIN_LF("Plain", true, 70),
	SKIRT("Skirt", true, 71),
	FLARES_F("Flares", true, 72),
	LONG_SKIRT("Long skirt", true, 73),
	LONG_NARROW_SKIRT("Long narrow skirt", true, 74),
	TATTY_LF("Tatty", true, 75),
	TURN_UPS_F("Turn-ups", true, 76),
	SHORT_SKIRT("Short skirt", true, 77),
	LAYERED_L("Layered", true, 78),
	SASH_AND_DOTS("Sash & dots", true, 135),
	BIG_HEM("Big hem", true, 136),
	SASH_AND_TROUSERS("Sash & trousers", true, 137),
	PATTERNED("Patterned", true, 138),
	TORN_SKIRT("Torn skirt", true, 139),
	PATCHED_SKIRT("Patched skirt", true, 140);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Override
	public KitType getKitType()
	{
		return KitType.LEGS;
	}

	@Override
	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public boolean isFemale()
	{
		return isFemale;
	}

	@Override
	public int getKitId()
	{
		return kitId;
	}
}
