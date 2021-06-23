package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum HandsKit implements Kit
{
	PLAIN_H("Plain", false, 34),
	BRACERS("Bracers", false, 35),

	PLAIN_HF("Plain", true, 68),
	BRACERS_F("Bracers", true, 69);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

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
