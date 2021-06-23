package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum TorsoKit implements Kit
{
	PLAIN("Plain", false, 18),
	LIGHT_BUTTONS("Light buttons", false, 19),
	DARK_BUTTONS("Dark buttons", false, 20),
	JACKET("Jacket", false, 21),
	SHIRT("Shirt", false, 22),
	STITCHING("Stitching", false, 23),
	TORN("Torn", false, 24),
	TWO_TONED("Two-toned", false, 25),
	SWEATER("Sweater", false, 105),
	BUTTONED_SHIRT("Buttoned shirt", false, 106),
	VEST("Vest", false, 107),
	PRINCELY_T("Princely", false, 108),
	RIPPED_WESKIT("Ripped weskit", false, 109),
	TORN_WESKIT("Torn weskit", false, 110),

	PLAIN_F("Plain", true, 56),
	CROP_TOP("Crop-top", true, 57),
	POLO_NECK("Polo-neck", true, 58),
	SIMPLE("Simple", true, 59),
	TORN_F("Torn", true, 60),
	SWEATER_F("Sweater", true, 89),
	SHIRT_F("Shirt", true, 90),
	VEST_F("Vest", true, 91),
	FRILLY("Frilly", true, 92),
	CORSETRY("Corsetry", true, 93),
	BODICE("Bodice", true, 94);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Override
	public KitType getKitType()
	{
		return KitType.TORSO;
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
