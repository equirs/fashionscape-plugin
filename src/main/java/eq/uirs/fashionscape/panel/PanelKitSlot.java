package eq.uirs.fashionscape.panel;

import eq.uirs.fashionscape.data.color.ColorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.Text;

@Getter
@RequiredArgsConstructor
public enum PanelKitSlot
{
	SKIN(null, ColorType.SKIN),
	HAIR(KitType.HAIR, ColorType.HAIR),
	JAW(KitType.JAW, ColorType.HAIR),
	TORSO(KitType.TORSO, ColorType.TORSO),
	ARMS(KitType.ARMS, ColorType.TORSO),
	HANDS(KitType.HANDS, null),
	LEGS(KitType.LEGS, ColorType.LEGS),
	BOOTS(KitType.BOOTS, ColorType.BOOTS),
	ICON(null, null);

	private final KitType kitType;

	private final ColorType colorType;

	public String getDisplayName()
	{
		return Text.titleCase(this);
	}
}
