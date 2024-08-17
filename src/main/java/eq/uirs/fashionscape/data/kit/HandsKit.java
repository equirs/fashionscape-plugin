package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
@Getter
public enum HandsKit implements Kit
{
	BRACERS2("Bracers", 33, 67, true),
	PLAIN_H("Plain", 34, 68, false),
	BRACERS("Bracers", 35, 69, false);

	private final String displayName;

	private final Integer mascKitId;
	private final Integer femKitId;

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
}
