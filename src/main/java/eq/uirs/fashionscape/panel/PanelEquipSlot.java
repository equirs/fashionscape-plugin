package eq.uirs.fashionscape.panel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum PanelEquipSlot
{
	ALL(null),
	HEAD(KitType.HEAD),
	AMULET(KitType.AMULET),
	CAPE(KitType.CAPE),
	TORSO(KitType.TORSO),
	WEAPON(KitType.WEAPON),
	SHIELD(KitType.SHIELD),
	HANDS(KitType.HANDS),
	LEGS(KitType.LEGS),
	BOOTS(KitType.BOOTS);

	@Getter
	private final KitType kitType;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}
}
