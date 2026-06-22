package eq.uirs.fashionscape.core.utils;

import eq.uirs.fashionscape.core.Fallbacks;
import eq.uirs.fashionscape.data.kit.Kit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.runelite.api.kit.KitType;

public class KitUtil
{
	public static final Map<Integer, Kit> KIT_ID_TO_KIT = new HashMap<>();
	public static final Map<KitType, List<Kit>> KIT_TYPE_TO_KITS;

	static
	{
		KIT_TYPE_TO_KITS = Arrays.stream(KitType.values())
			.collect(Collectors.toMap(s -> s, s -> Arrays.asList(Kit.allInSlot(s, true))));
		for (KitType slot : KitType.values())
		{
			for (Kit value : Kit.allInSlot(slot, true))
			{
				Integer mascId = value.getKitId(0);
				if (mascId != null)
				{
					KIT_ID_TO_KIT.put(mascId, value);
				}
				Integer femId = value.getKitId(1);
				if (femId != null)
				{
					KIT_ID_TO_KIT.put(femId, value);
				}
			}
		}
	}

	@Nullable
	public static Kit getWithAnalog(int kitId, Integer gender)
	{
		Kit kit = KIT_ID_TO_KIT.get(kitId);
		if (kit == null)
		{
			return null;
		}
		if (kit.getKitId(gender) == null)
		{
			kit = Fallbacks.getMirroredKit(kit, gender);
		}
		return kit;
	}
}
