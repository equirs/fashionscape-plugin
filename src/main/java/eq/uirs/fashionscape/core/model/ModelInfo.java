package eq.uirs.fashionscape.core.model;

import eq.uirs.fashionscape.core.event.IconChanged;
import eq.uirs.fashionscape.core.layer.ModelType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import javax.annotation.Nullable;
import lombok.Data;
import net.runelite.api.kit.KitType;
import net.runelite.client.eventbus.EventBus;

/**
 * Combined representation of all fashionscape-related model state: items, kits, colors, and icon.
 * Each ModelType maps to one instance of ModelInfo.
 */
@Data
public class ModelInfo
{
	private final Items items;
	private final Kits kits;
	private final Colors colors;
	@Nullable
	private JawIcon icon;

	private final ModelType modelType;
	private final EventBus eventBus;

	public ModelInfo(ModelType modelType, EventBus eventBus)
	{
		items = new Items(modelType, eventBus);
		kits = new Kits(modelType, eventBus);
		colors = new Colors(modelType, eventBus);
		this.eventBus = eventBus;
		this.modelType = modelType;
	}

	public JawIcon putIcon(@Nullable JawIcon icon)
	{
		JawIcon oldIcon = this.icon;
		this.icon = icon;
		eventBus.post(new IconChanged(icon, modelType));
		return oldIcon;
	}

	public boolean contains(KitType slot)
	{
		return items.containsKey(slot) || kits.containsKey(slot);
	}
}
