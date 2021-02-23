package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.ColorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.Text;

@RequiredArgsConstructor
public enum PanelKitSlot
{
	SKIN(null, ColorType.SKIN),
	HAIR(KitType.HAIR, ColorType.HAIR),
	JAW(KitType.JAW, null),
	TORSO(KitType.TORSO, ColorType.TORSO),
	ARMS(KitType.ARMS, null),
	HANDS(KitType.HANDS, null),
	LEGS(KitType.LEGS, ColorType.LEGS),
	BOOTS(KitType.BOOTS, ColorType.BOOTS);

	@Getter
	private final KitType kitType;

	@Getter
	private final ColorType colorType;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}
}
