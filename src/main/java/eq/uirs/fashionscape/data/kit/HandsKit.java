package eq.uirs.fashionscape.data.kit;

import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum HandsKit implements Kit
{
	BRACERS2("Bracers", 33, 67, true),
	PLAIN_H("Plain", 34, 68, false),
	BRACERS("Bracers", 35, 69, false);

	private final String displayName;

	private final int maleKitId;

	private final int femaleKitId;

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
	public boolean isHidden()
	{
		return hidden;
	}

	@Override
	public Integer getKitId(boolean isFemale)
	{
		return isFemale ? femaleKitId : maleKitId;
	}
}
