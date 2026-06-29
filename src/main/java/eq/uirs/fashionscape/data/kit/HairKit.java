package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@Getter
public enum HairKit implements Kit
{
	BALD(0, 45),
	DREADLOCKS(1, 47),
	LONG(2, 48),
	MEDIUM(3, 49),
	TONSURE(4, 174),
	SHORT(5, 51),
	CROPPED(6, 52),
	WILD_SPIKES(7, 53),
	SPIKES(8, 54),
	MOHAWK(9, 175),
	WIND_BRAIDS(129, 120),
	QUIFF(130, 176),
	SAMURAI(131, 177),
	PRINCELY(132, 178),
	CURTAINS(133, 128),
	LONG_CURTAINS(134, 179),
	TOUSLED(144, 180),
	SIDE_WEDGE(145, 181),
	FRONT_WEDGE(146, 182),
	FRONT_SPIKES(147, 183),
	FROHAWK(148, 184),
	REAR_SKIRT(149, 185),
	QUEUE(150, 186),
	FRONT_SPLIT(151, 141),
	MULLET(201, 154),
	UNDERCUT(202, 155),
	POMPADOUR(203, 156),
	AFRO(204, 157),
	SHORT_LOCS(205, 158),
	SPIKY_MOHAWK(206, 159),
	SLICKED_MOHAWK(207, 160),
	LONG_QUIFF(208, 161),
	SHORT_CHOPPY(209, 162),
	SIDE_AFRO(210, 163),
	PUNK(211, 164),
	HALF_SHAVED(212, 165),
	FREMENNIK(213, 166),
	ELVEN(214, 167),
	MEDIUM_COILS(215, 168),
	LOW_BUN(216, 169),
	MESSY_BUN(217, 170),
	HIGH_PONYTAIL(218, 171),
	PLAITS(219, 172),
	HIGH_BUNCHES(220, 173),
	BUN(221, 46),
	PIGTAILS(222, 50),
	EARMUFFS(223, 55),
	SIDE_PONY(224, 118),
	CURLS(225, 119),
	PONYTAIL(226, 121),
	BRAIDS(227, 122),
	BUNCHES(228, 123),
	BOB(229, 124),
	LAYERED(230, 125),
	STRAIGHT(231, 126),
	STRAIGHT_BRAIDS(232, 127),
	TWO_BACK(233, 143);

	// TODO support hat hair ids
	private final Integer mascKitId;
	private final Integer femKitId;

	@Override
	public KitType getKitType()
	{
		return KitType.HAIR;
	}

	@Override
	public String getDisplayName()
	{
		switch (this)
		{
			case HALF_SHAVED:
				return "Half-shaved";
			case TWO_BACK:
				return "Two-back";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
