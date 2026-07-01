package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@Getter
@RequiredArgsConstructor
public enum ArmsKit implements Kit
{
	REGULAR(26, 61),
	MUSCLY(27, 63),
	LOOSE_SLEEVED(28, 248),
	LARGE_CUFFS(29, 65),
	THIN_SLEEVES(30, 249),
	SHOULDER_PADS(31, 250),
	THIN_STRIPE(32, 97),
	THICK_STRIPES(84, 95),
	WHITE_CUFFS(85, 96),
	REGAL_A(86, 251),
	TATTY(87, 252),
	TATTY_SHOULDERS(263, 98),
	RIPPED(88, 253),
	BARE_SLEEVES(260, 62),
	LONG_SLEEVED(261, 64),
	FRILLY_A(262, 66),
	BARE_SHOULDERS(264, 99);

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
			case REGAL_A:
				return "Regal";
			case FRILLY_A:
				return "Frilly";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
