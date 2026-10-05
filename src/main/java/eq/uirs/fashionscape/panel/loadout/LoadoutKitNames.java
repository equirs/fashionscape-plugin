package eq.uirs.fashionscape.panel.loadout;

import eq.uirs.fashionscape.core.loadout.Loadout;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.kit.Kit;
import eq.uirs.fashionscape.panel.PanelKitSlot;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import net.runelite.api.kit.KitType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Lists a loadout's kit names in Base tab order, for loadouts without item icons.
 */
class LoadoutKitNames extends JLabel
{
	LoadoutKitNames(List<String> names)
	{
		super(String.join(", ", names));
		setFont(FontManager.getRunescapeSmallFont());
		setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		// zero width lets the text truncate instead of widening the row
		setPreferredSize(new Dimension(0, getPreferredSize().height));
	}

	static List<String> namesOf(Loadout loadout)
	{
		List<String> names = new ArrayList<>();
		for (PanelKitSlot panelSlot : PanelKitSlot.values())
		{
			KitType slot = panelSlot.getKitType();
			Integer kitId = slot != null ? loadout.getKits().get(slot) : null;
			Kit kit = kitId != null ? KitUtil.KIT_ID_TO_KIT.get(kitId) : null;
			if (kit != null)
			{
				names.add(kit.getDisplayName());
			}
		}
		return names;
	}
}
