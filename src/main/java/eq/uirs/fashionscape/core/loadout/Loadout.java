package eq.uirs.fashionscape.core.loadout;

import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import net.runelite.api.kit.KitType;

/**
 * A saved player appearance. Slots missing from the maps are left unset when the loadout is applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loadout
{
	// item id for a slot that explicitly shows nothing
	public static final int NOTHING = -1;

	@With
	private String name = "";
	private Map<KitType, Integer> items = new HashMap<>();
	private Map<KitType, Integer> kits = new HashMap<>();
	private Map<ColorType, Integer> colors = new HashMap<>();
	@Nullable
	private JawIcon icon;

	public boolean isEmpty()
	{
		return items.isEmpty() && kits.isEmpty() && colors.isEmpty() && icon == null;
	}
}
