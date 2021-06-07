package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum HairKit implements Kit
{
	BALD("Bald", false, 0),
	DREADLOCKS("Dreadlocks", false, 1),
	LONG("Long", false, 2),
	MEDIUM("Medium", false, 3),
	TONSURE("Tonsure", false, 4),
	SHORT("Short", false, 5),
	CROPPED("Cropped", false, 6),
	WILD_SPIKES("Wild spikes", false, 7),
	SPIKES("Spikes", false, 8),
	MOHAWK("Mohawk", false, 9),
	WIND_BRAIDS("Wind braids", false, 129),
	QUIFF("Quiff", false, 130),
	SAMURAI("Samurai", false, 131),
	PRINCELY("Princely", false, 132),
	CURTAINS("Curtains", false, 133),
	LONG_CURTAINS("Long curtains", false, 134),
	TOUSLED("Tousled", false, 144),
	SIDE_WEDGE("Side wedge", false, 145),
	FRONT_WEDGE("Front wedge", false, 146),
	FRONT_SPIKES("Front spikes", false, 147),
	FROHAWK("Frohawk", false, 148),
	REAR_SKIRT("Rear skirt", false, 149),
	QUEUE("Queue", false, 150),
	FRONT_SPLIT("Front split", false, 151),

	BALD_F("Bald", true, 45),
	BUN("Bun", true, 46),
	DREADLOCKS_F("Dreadlocks", true, 47),
	LONG_F("Long", true, 48),
	MEDIUM_F("Medium", true, 49),
	PIGTAILS("Pigtails", true, 50),
	SHORT_F("Short", true, 51),
	CROPPED_F("Cropped", true, 52),
	WILD_SPIKES_F("Wild spikes", true, 53),
	SPIKY("Spiky", true, 54),
	EARMUFFS("Earmuffs", true, 55),
	SIDE_PONY("Side pony", true, 118),
	CURLS("Curls", true, 119),
	WIND_BRAIDS_F("Wind braids", true, 120),
	PONYTAIL("Ponytail", true, 121),
	BRAIDS("Braids", true, 122),
	BUNCHES("Bunches", true, 123),
	BOB("Bob", true, 124),
	LAYERED("Layered", true, 125),
	STRAIGHT("Straight", true, 126),
	STRAIGHT_BRAIDS("Straight braids", true, 127),
	CURTAINS_F("Curtains", true, 128),
	FRONT_SPLIT_F("Front split", true, 141),
	TWO_BACK("Two-back", true, 143);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Override
	public KitType getKitType()
	{
		return KitType.HAIR;
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
