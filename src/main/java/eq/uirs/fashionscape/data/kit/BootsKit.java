package eq.uirs.fashionscape.data.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;

@RequiredArgsConstructor
public enum BootsKit implements Kit
{
	SMALL("Small", false, 42, false),
	LARGE("Large", false, 43, false),
	LARGE_2("Large", false, 44, true),

	SMALL_F("Small", true, 79, false),
	LARGE_F("Large", true, 80, false),
	LARGE_F2("Large", true, 81, true),

	MINECART("Minecart", false, 82, true),
	MINECART_F("Minecart", false, 83, true);

	private final String displayName;

	private final boolean isFemale;

	private final int kitId;

	@Getter
	private final boolean hidden;

	@Override
	public KitType getKitType()
	{
		return KitType.BOOTS;
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
