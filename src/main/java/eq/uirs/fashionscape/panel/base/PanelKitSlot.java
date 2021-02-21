package eq.uirs.fashionscape.panel.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum PanelKitSlot
{
	HAIR(KitType.HAIR),
	FACIAL_HAIR(KitType.JAW),
	BODY(KitType.TORSO),
	ARMS(KitType.ARMS),
	HANDS(KitType.HANDS),
	LEGS(KitType.LEGS),
	FEET(KitType.BOOTS);

	@Getter
	private final KitType kitType;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}
}
