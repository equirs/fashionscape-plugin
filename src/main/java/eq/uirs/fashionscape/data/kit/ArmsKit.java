package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum ArmsKit implements Kit
{
	REGULAR("Regular", false, 26),
	MUSCLEBOUND("Musclebound", false, 27),
	LOOSE_SLEEVED("Loose sleeved", false, 28),
	LARGE_CUFFED("Large cuffed", false, 29),
	THIN("Thin", false, 30),
	SHOULDER_PADS("Shoulder pads", false, 31),
	THIN_STRIPE("Thin stripe", false, 32),
	THICK_STRIPE("Thick stripe", false, 84),
	WHITE_CUFFS("White cuffs", false, 85),
	PRINCELY_A("Princely", false, 86),
	TATTY("Tatty", false, 87),
	RIPPED("Ripped", false, 88),

	SHORT_SLEEVES("Short sleeves", true, 61),
	BARE_ARMS("Bare arms", true, 62),
	MUSCLEY("Muscley", true, 63),
	LONG_SLEEVED("Long sleeved", true, 64),
	LARGE_CUFFS("Large cuffs", true, 65),
	FRILLY_A("Frilly", true, 66),
	SWEATER_A("Sweater", true, 95),
	WHITE_CUFFS_F("White cuffs", true, 96),
	THIN_STRIPE_F("Thin stripe", true, 97),
	TATTY_F("Tatty", true, 98),
	BARE_SHOULDERS("Bare shoulders", true, 99);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Override
	public KitType getKitType()
	{
		return KitType.ARMS;
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
