package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@Getter
@RequiredArgsConstructor
public enum BootsKit implements Kit
{
	SMALL(42, 79, false),
	LARGE(43, 80, false),
	LARGE_2(44, 81, true),
	MINECART(82, 83, true);

	private final Integer mascKitId;
	private final Integer femKitId;

	private final boolean hidden;

	@Override
	public KitType getKitType()
	{
		return KitType.BOOTS;
	}

	@Override
	public String getDisplayName()
	{
		if (this == LARGE_2)
		{
			return "Large";
		}
		return Kit.sentenceCaseName(this);
	}
}
