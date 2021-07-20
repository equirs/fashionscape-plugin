package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum HandsKit implements Kit
{
	BRACERS2("Bracers", false, 33, true),
	PLAIN_H("Plain", false, 34, false),
	BRACERS("Bracers", false, 35, false),

	BRACERS_F2("Bracers", true, 67, true),
	PLAIN_HF("Plain", true, 68, false),
	BRACERS_F("Bracers", true, 69, false);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Getter
	private final boolean hidden;

	@Override
	public KitType getKitType()
	{
		return KitType.HANDS;
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
