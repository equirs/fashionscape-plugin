package eq.uirs.fashionscape.data.kit;

import java.util.Arrays;
import net.runelite.api.kit.KitType;

public interface Kit
{
	static Kit[] allInSlot(KitType slot, boolean includeHidden)
	{
		switch (slot)
		{
			case HAIR:
				return HairKit.values();
			case JAW:
				return JawKit.values();
			case TORSO:
				return TorsoKit.values();
			case ARMS:
				return ArmsKit.values();
			case LEGS:
				return LegsKit.values();
			case HANDS:
				return HandsKit.values();
			case BOOTS:
				return Arrays.stream(BootsKit.values())
					.filter(kit -> includeHidden || !kit.isHidden())
					.toArray(BootsKit[]::new);
		}
		return new Kit[0];
	}

	KitType getKitType();

	String getDisplayName();

	boolean isFemale();

	int getKitId();
}
