package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@Getter
public enum TorsoKit implements Kit
{
	PLAIN(18, 56),
	LIGHT_BUTTONS(19, null),
	DARK_BUTTONS(20, null),
	JACKET(21, null),
	SHIRT(22, 90),
	STITCHING(23, null),
	TORN(24, 60),
	TWO_TONED(25, null),
	SWEATER(105, 89),
	BUTTONED_SHIRT(106, null),
	VEST(107, 91),
	PRINCELY_T(108, null),
	RIPPED_WESKIT(109, null),
	TORN_WESKIT(110, null),
	CROP_TOP(null, 57),
	POLO_NECK(null, 58),
	SIMPLE(null, 59),
	FRILLY(null, 92),
	CORSETRY(null, 93),
	BODICE(null, 94);

	private final Integer mascKitId;
	private final Integer femKitId;

	@Override
	public KitType getKitType()
	{
		return KitType.TORSO;
	}

	@Override
	public String getDisplayName()
	{
		switch (this)
		{
			case TWO_TONED:
				return "Two-toned";
			case PRINCELY_T:
				return "Princely";
			case CROP_TOP:
				return "Crop-top";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
