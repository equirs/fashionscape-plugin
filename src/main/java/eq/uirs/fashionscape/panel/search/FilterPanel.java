package eq.uirs.fashionscape.panel.search;

import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.event.LockChanged;
import eq.uirs.fashionscape.core.filter.ItemFilters;
import eq.uirs.fashionscape.core.filter.Membership;
import eq.uirs.fashionscape.core.filter.TriState;
import eq.uirs.fashionscape.core.filter.ValueSource;
import eq.uirs.fashionscape.panel.PanelUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.kit.KitType;
import net.runelite.client.ui.ColorScheme;

/**
 * Expandable panel of search filters. Toggle button is placed by the parent panel.
 */
class FilterPanel extends JPanel
{
	private static final Color BACKGROUND = ColorScheme.DARKER_GRAY_HOVER_COLOR;

	@Getter
	private final JToggleButton toggle = new JToggleButton();
	private final SlotTabs slotTabs;
	private final ValueRangeFields range;
	private final JPanel rows = new JPanel(new GridBagLayout());
	private final GridBagConstraints rowConstraints = new GridBagConstraints();
	// updates a control when changing filters
	private final List<Consumer<ItemFilters>> syncs = new ArrayList<>();
	private final Consumer<KitType> onSlotChange;
	private final Consumer<ItemFilters> onFiltersChange;

	// null -> any slot
	@Nullable
	private KitType slot;
	private ItemFilters filters;
	// true while controls are updated programmatically, so listeners don't report changes
	private boolean syncing;

	FilterPanel(FashionManager fashionManager, ItemFilters initialFilters,
				Consumer<KitType> onSlotChange, Consumer<ItemFilters> onFiltersChange)
	{
		this.filters = initialFilters;
		this.onSlotChange = onSlotChange;
		this.onFiltersChange = onFiltersChange;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 0, 0, 0));

		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBackground(BACKGROUND);
		content.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(content, BorderLayout.CENTER);

		slotTabs = new SlotTabs(fashionManager, this::setSlot);
		slotTabs.setBackground(BACKGROUND);
		content.add(slotTabs, BorderLayout.NORTH);

		range = new ValueRangeFields((min, max) -> setFilters(filters.withMinValue(min).withMaxValue(max)));
		rows.setOpaque(false);
		addFilterRows();
		content.add(rows, BorderLayout.CENTER);
		content.add(createResetButton(), BorderLayout.SOUTH);

		toggle.setFocusPainted(false);
		toggle.setHorizontalTextPosition(SwingConstants.LEADING);
		toggle.addMouseListener(PanelUtil.hoverCursor(toggle));
		toggle.addActionListener(e -> refreshExpanded());
		refreshExpanded();

		sync(initialFilters);
		slotTabs.selectSlot(null);
	}

	void selectSlot(@Nullable KitType slot)
	{
		slotTabs.selectSlot(slot);
	}

	void onLockChanged(LockChanged e)
	{
		slotTabs.onLockChanged(e);
	}

	void reset()
	{
		sync(ItemFilters.DEFAULT);
		setFilters(ItemFilters.DEFAULT);
		slotTabs.selectSlot(null);
	}

	private void addFilterRows()
	{
		rowConstraints.fill = GridBagConstraints.HORIZONTAL;
		rowConstraints.gridy = 0;
		addRow("Members", createComboBox(Membership.values(),
			ItemFilters::getMembership, ItemFilters::withMembership));
		addRow("Tradeable", createComboBox(TriState.values(),
			ItemFilters::getTradeable, ItemFilters::withTradeable));
		addRow("Has stats", createComboBox(TriState.values(),
			ItemFilters::getCombatStats, ItemFilters::withCombatStats));
		addRow("Value", createComboBox(ValueSource.values(),
			ItemFilters::getValueSource, ItemFilters::withValueSource));

		syncs.add(f -> range.setRange(f.getMinValue(), f.getMaxValue()));
		syncs.add(this::refreshRangeVisible);
		rowConstraints.gridx = 0;
		rowConstraints.gridwidth = 2;
		rowConstraints.insets = new Insets(5, 0, 0, 0);
		rows.add(range, rowConstraints);
	}

	private void addRow(String text, JComponent control)
	{
		int top = rowConstraints.gridy == 0 ? 0 : 5;
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		rowConstraints.gridx = 0;
		rowConstraints.weightx = 0;
		rowConstraints.insets = new Insets(top, 0, 0, 5);
		rows.add(label, rowConstraints);
		rowConstraints.gridx = 1;
		rowConstraints.weightx = 1;
		rowConstraints.insets = new Insets(top, 0, 0, 0);
		rows.add(control, rowConstraints);
		rowConstraints.gridy++;
	}

	private <T> JComboBox<T> createComboBox(T[] values, Function<ItemFilters, T> getter,
											BiFunction<ItemFilters, T, ItemFilters> transform)
	{
		JComboBox<T> box = new JComboBox<>(values);
		box.setFocusable(false);
		box.setForeground(Color.WHITE);
		box.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				T newValue = box.getItemAt(box.getSelectedIndex());
				setFilters(transform.apply(filters, newValue));
			}
		});
		syncs.add(f -> box.setSelectedItem(getter.apply(f)));
		return box;
	}

	private JButton createResetButton()
	{
		JButton button = new JButton("Reset");
		button.setFocusPainted(false);
		button.addMouseListener(PanelUtil.hoverCursor(button));
		button.addActionListener(e -> reset());
		return button;
	}

	private void sync(ItemFilters shown)
	{
		syncing = true;
		syncs.forEach(s -> s.accept(shown));
		syncing = false;
	}

	private void setFilters(ItemFilters newFilters)
	{
		if (syncing || newFilters.equals(filters))
		{
			return;
		}
		filters = newFilters;
		refreshRangeVisible(newFilters);
		refreshToggleText();
		onFiltersChange.accept(newFilters);
	}

	private void refreshRangeVisible(ItemFilters shown)
	{
		range.setVisible(shown.getValueSource() != ValueSource.ANY);
	}

	private void setSlot(@Nullable KitType newSlot)
	{
		slot = newSlot;
		refreshToggleText();
		onSlotChange.accept(newSlot);
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
		int count = (slot != null ? 1 : 0) + filters.activeCount();
		toggle.setText(count == 0 ? "Filters" : "Filters (" + count + ")");
	}
}
