package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@Getter
@RequiredArgsConstructor
public enum ArmsKit implements Kit
{
	REGULAR(26, null),
	MUSCLEBOUND(27, null),
	LOOSE_SLEEVED(28, null),
	LARGE_CUFFED(29, null),
	THIN(30, null),
	SHOULDER_PADS(31, null),
	THIN_STRIPE(32, 97),
	THICK_STRIPE(84, null),
	WHITE_CUFFS(85, 96),
	PRINCELY_A(86, null),
	TATTY(87, 98),
	RIPPED(88, null),
	SHORT_SLEEVES(null, 61),
	BARE_ARMS(null, 62),
	MUSCLEY(null, 63),
	LONG_SLEEVED(null, 64),
	LARGE_CUFFS(null, 65),
	FRILLY_A(null, 66),
	SWEATER_A(null, 95),
	BARE_SHOULDERS(null, 99);

	private final Integer mascKitId;
	private final Integer femKitId;

	@Override
	public KitType getKitType()
	{
		return KitType.ARMS;
	}

	@Override
	public String getDisplayName()
	{
		switch (this)
		{
			case PRINCELY_A:
				return "Princely";
			case FRILLY_A:
				return "Frilly";
			case SWEATER_A:
				return "Sweater";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
