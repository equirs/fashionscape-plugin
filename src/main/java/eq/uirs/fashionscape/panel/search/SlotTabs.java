package eq.uirs.fashionscape.panel.search;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import net.runelite.api.kit.KitType;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * Grid of equipment slot tabs with lock badges
 */
class SlotTabs extends MaterialTabGroup
{
	private final Map<PanelEquipSlot, MaterialTab> tabMap;

	// onSelect receives null for the "all" tab
	SlotTabs(FashionManager fashionManager, Consumer<KitType> onSelect)
	{
		setLayout(new GridLayout(2, 5, 5, 5));
		ImmutableMap.Builder<PanelEquipSlot, MaterialTab> builder = new ImmutableMap.Builder<>();
		for (PanelEquipSlot panelSlot : PanelEquipSlot.values())
		{
			KitType kitType = panelSlot.getKitType();
			boolean isLocked = kitType != null && fashionManager.isItemLocked(kitType);
			MaterialTab tab = new MaterialTab(iconFor(panelSlot, isLocked), this, null);
			tab.setToolTipText(panelSlot.getDisplayName());
			tab.addMouseListener(PanelUtil.hoverCursor(tab));
			tab.setOnSelectEvent(() -> {
				onSelect.accept(kitType);
				return true;
			});
			addTab(tab);
			builder.put(panelSlot, tab);
		}
		tabMap = builder.build();
	}

	void selectSlot(@Nullable KitType slot)
	{
		select(tabMap.get(panelSlotFor(slot)));
	}

	void onLockChanged(LockChanged e)
	{
		PanelEquipSlot panelSlot = panelSlotFor(e.getSlot());
		if (panelSlot != PanelEquipSlot.ALL)
		{
			tabMap.get(panelSlot).setIcon(iconFor(panelSlot, e.getStatus() != null));
		}
	}

	// slots without a tab (e.g. hair) map to "all"
	private static PanelEquipSlot panelSlotFor(@Nullable KitType slot)
	{
		return Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.orElse(PanelEquipSlot.ALL);
	}

	private static ImageIcon iconFor(PanelEquipSlot panelSlot, boolean isLocked)
	{
		String lockStr = isLocked ? "-lock" : "";
		return PanelUtil.icon(panelSlot.getDisplayName().toLowerCase() + lockStr);
	}
}
