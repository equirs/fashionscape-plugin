package eq.uirs.fashionscape.data.kit;

import java.util.Arrays;
import java.util.function.IntFunction;
import net.runelite.api.kit.KitType;
import org.apache.commons.text.WordUtils;

public interface Kit
{
	static Kit[] allInSlot(KitType slot, boolean includeHidden)
	{
		Kit[] values;
		IntFunction<Kit[]> creator;
		switch (slot)
		{
			case HAIR:
				values = HairKit.values();
				creator = HairKit[]::new;
				break;
			case JAW:
				values = JawKit.values();
				creator = JawKit[]::new;
				break;
			case TORSO:
				values = TorsoKit.values();
				creator = TorsoKit[]::new;
				break;
			case ARMS:
				values = ArmsKit.values();
				creator = ArmsKit[]::new;
				break;
			case LEGS:
				values = LegsKit.values();
				creator = LegsKit[]::new;
				break;
			case HANDS:
				values = HandsKit.values();
				creator = HandsKit[]::new;
				break;
			case BOOTS:
				values = BootsKit.values();
				creator = BootsKit[]::new;
				break;
			default:
				return new Kit[0];
		}
		return Arrays.stream(values)
			.filter(kit -> includeHidden || !kit.isHidden())
			.toArray(creator);
	}

	KitType getKitType();

	String getDisplayName();

	boolean isHidden();

	Integer getMascKitId();
	Integer getFemKitId();

	default Integer getKitId(Integer gender)
	{
		switch (gender)
		{
			case 0:
				return getMascKitId();
			case 1:
				return getFemKitId();
			default:
				return null;
		}
	}

	static String sentenceCaseName(Enum e)
	{
		return WordUtils.capitalize(e.toString().toLowerCase()).replace("_", " ");
	}
}
