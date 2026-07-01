package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@Getter
public enum TorsoKit implements Kit
{
	PLAIN(18, 56),
	LIGHT_BUTTONS(19, 265),
	DARK_BUTTONS(20, 266),
	JACKET(21, 267),
	SHIRT(22, 268),
	STITCHING(23, 269),
	TORN(24, 60),
	TWO_TONED(25, 270),
	SWEATER(105, 89),
	CUFFED_SHIRT(106, 90),
	VEST(107, 91),
	REGAL(108, 271),
	RIPPED_WESKIT(109, 272),
	TORN_WESKIT(110, 273),
	CROP_TOP(254, 57),
	POLO_NECK(255, 58),
	SIMPLE(256, 59),
	FRILLY(257, 92),
	CORSETRY(258, 93),
	BODICE(259, 94);

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
		if (this == TorsoKit.TWO_TONED)
		{
			return "Two-toned";
		}
		return Kit.sentenceCaseName(this);
	}

	@Override
	public boolean isHidden()
	{
		return false;
	}
}
