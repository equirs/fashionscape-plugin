package eq.uirs.fashionscape.panel.search;

import com.google.common.collect.ImmutableMap;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.panel.PanelEquipSlot;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.kit.KitType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * Expandable panel of search filters. Toggle button is placed by the parent panel.
 */
class FilterPanel extends JPanel
{
	private static final Color BACKGROUND = ColorScheme.DARKER_GRAY_HOVER_COLOR;

	@Getter
	private final JToggleButton toggle = new JToggleButton();
	private final MaterialTabGroup slotTabs = new MaterialTabGroup();
	private final Map<PanelEquipSlot, MaterialTab> tabMap;
	private final FashionManager fashionManager;
	private final Consumer<KitType> onSlotChange;

	// null -> any slot
	@Nullable
	private KitType slot;

	FilterPanel(FashionManager fashionManager, Consumer<KitType> onSlotChange)
	{
		this.fashionManager = fashionManager;
		this.onSlotChange = onSlotChange;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 0, 0, 0));

		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(BACKGROUND);
		content.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(content, BorderLayout.CENTER);

		tabMap = createSlotTabs();
		content.add(slotTabs, BorderLayout.NORTH);

		toggle.setFocusPainted(false);
		toggle.setHorizontalTextPosition(SwingConstants.LEADING);
		toggle.addMouseListener(PanelUtil.hoverCursor(toggle));
		toggle.addActionListener(e -> refreshExpanded());
		refreshExpanded();
		refreshToggleText();
	}

	void selectSlot(@Nullable KitType slot)
	{
		PanelEquipSlot panelSlot = Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == slot)
			.findFirst()
			.orElse(PanelEquipSlot.ALL);
		slotTabs.select(tabMap.get(panelSlot));
	}

	void onLockChanged(LockChanged e)
	{
		Arrays.stream(PanelEquipSlot.values())
			.filter(p -> p.getKitType() == e.getSlot())
			.findFirst()
			.ifPresent(p -> tabMap.get(p).setIcon(iconFor(p, e.getStatus() != null)));
	}

	private Map<PanelEquipSlot, MaterialTab> createSlotTabs()
	{
		ImmutableMap.Builder<PanelEquipSlot, MaterialTab> builder = new ImmutableMap.Builder<>();
		slotTabs.setLayout(new GridLayout(2, 5, 5, 5));
		slotTabs.setBackground(BACKGROUND);
		for (PanelEquipSlot panelSlot : PanelEquipSlot.values())
		{
			KitType kitType = panelSlot.getKitType();
			boolean isLocked = kitType != null && fashionManager.isItemLocked(kitType);
			MaterialTab tab = new MaterialTab(iconFor(panelSlot, isLocked), slotTabs, null);
			tab.setToolTipText(panelSlot.getDisplayName());
			tab.addMouseListener(PanelUtil.hoverCursor(this));
			tab.setOnSelectEvent(() -> {
				slot = kitType;
				refreshToggleText();
				onSlotChange.accept(kitType);
				return true;
			});
			slotTabs.addTab(tab);
			builder.put(panelSlot, tab);
		}
		Map<PanelEquipSlot, MaterialTab> tabs = builder.build();
		slotTabs.select(tabs.get(PanelEquipSlot.ALL));
		return tabs;
	}

	private void refreshExpanded()
	{
		boolean expanded = toggle.isSelected();
		setVisible(expanded);
		toggle.setIcon(PanelUtil.icon(expanded ? "up" : "down"));
		toggle.setToolTipText(expanded ? "Hide filters" : "Show filters");
	}

	private void refreshToggleText()
	{
		int count = slot != null ? 1 : 0;
		toggle.setText(count == 0 ? "Filters" : "Filters (" + count + ")");
	}

	private static ImageIcon iconFor(PanelEquipSlot panelSlot, boolean isLocked)
	{
		String lockStr = isLocked ? "-lock" : "";
		return PanelUtil.icon(panelSlot.getDisplayName().toLowerCase() + lockStr);
	}
}
