package eq.uirs.fashionscape.core.model;

import eq.uirs.fashionscape.core.Events;
import eq.uirs.fashionscape.core.event.IconChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import lombok.Data;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.Nullable;

/**
 * Combined representation of all fashionscape-related model info: items, kits, colors, and icon.
 */
@Data
public class ModelInfo
{
	private final Items items;
	private final Kits kits;
	private final Colors colors;
	private final ModelType modelType;
	@Nullable
	private JawIcon icon;

	public ModelInfo(ModelType modelType)
	{
		items = new Items(modelType);
		kits = new Kits(modelType);
		colors = new Colors(modelType);
		this.modelType = modelType;
	}

	public JawIcon putIcon(@Nullable JawIcon icon)
	{
		JawIcon oldIcon = this.icon;
		this.icon = icon;
		Events.fire(new IconChanged(icon, modelType));
		return oldIcon;
	}

	public boolean contains(KitType slot)
	{
		return items.containsKey(slot) || kits.containsKey(slot);
	}
}
