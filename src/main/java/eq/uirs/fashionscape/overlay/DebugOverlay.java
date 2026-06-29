package eq.uirs.fashionscape.overlay;

import com.google.common.collect.ImmutableList;
import eq.uirs.fashionscape.core.FashionManager;
import eq.uirs.fashionscape.core.SlotInfo;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.model.ModelInfo;
import eq.uirs.fashionscape.core.utils.KitUtil;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.color.Colorable;
import eq.uirs.fashionscape.data.kit.Kit;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;
import net.runelite.client.ui.overlay.OverlayPanel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Displays current Layers state in an overlay. Only displays in developer mode.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DebugOverlay extends OverlayPanel
{
	private final FashionManager fashionManager;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Layers layers = fashionManager.getLayers();
		ModelInfo realModels = layers.getRealModels();
		ModelInfo virtualModels = layers.getVirtualModels();
		ModelInfo previewModels = layers.getPreviewModels();

		dumpItems("Real items", realModels);
		dumpItems("Plugin items", virtualModels);
		dumpItems("Hover items", previewModels);

		dumpKits("Real kits", realModels);
		dumpKits("Plugin kits", virtualModels);
		dumpKits("Hover kits", previewModels);

		dumpColors("Real colors", realModels);
		dumpColors("Plugin colors", virtualModels);
		dumpColors("Hover colors", previewModels);

		panelComponent.getChildren().add(MonospaceTitleComponent.builder()
			.text("")
			.build());
		panelComponent.getChildren().add(MonospaceTitleComponent.builder()
			.text("real idle id " + layers.getLastRealIdlePoseAnim())
			.build());
		panelComponent.getChildren().add(MonospaceTitleComponent.builder()
			.text("compute idle " + layers.computeIdlePoseAnimation())
			.build());
		panelComponent.getChildren().add(MonospaceTitleComponent.builder()
			.text("gender " + layers.getGender())
			.build());

		return super.render(graphics);
	}

	private void dumpItems(String title, ModelInfo models)
	{
		if (models.getItems().getAll().isEmpty())
		{
			return;
		}
		addNewlineAndTitle(title);
		List<List<String>> cells = models.getItems().getAll().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(e -> itemCells(e.getKey(), e.getValue()))
			.collect(Collectors.toList());
		dumpCells(cells);
	}

	private void dumpKits(String title, ModelInfo models)
	{
		if (models.getKits().getAll().isEmpty())
		{
			return;
		}
		addNewlineAndTitle(title);
		List<List<String>> kitCells = models.getKits().getAll().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(e -> kitCells(e.getKey(), e.getValue()))
			.collect(Collectors.toList());
		dumpCells(kitCells);
	}

	private void dumpColors(String title, ModelInfo models)
	{
		if (models.getColors().getAll().isEmpty())
		{
			return;
		}
		addNewlineAndTitle(title);
		List<List<String>> colorCells = models.getColors().getAllColorable().entrySet().stream()
			.map(e -> colorCells(e.getKey(), e.getValue()))
			.collect(Collectors.toList());
		dumpCells(colorCells);
	}

	private void dumpCells(List<List<String>> cells)
	{
		if (!cells.isEmpty())
		{
			int[] maxLengths = cells.stream().reduce(
				new int[cells.get(0).size()],
				(lengths, row) -> {
					for (int i = 0; i < row.size(); i++)
					{
						lengths[i] = Integer.max(row.get(i).length(), lengths[i]);
					}
					return lengths;
				},
				ArrayUtils::addAll);
			for (List<String> row : cells)
			{
				List<String> paddedRow = IntStream.range(0, row.size())
					.boxed()
					.map(i -> StringUtils.rightPad(row.get(i), maxLengths[i]))
					.collect(Collectors.toList());
				panelComponent.getChildren().add(MonospaceTitleComponent.builder()
					.text(String.join(" ", paddedRow))
					.build());
			}
		}
	}

	// slot, itemId, hideSlots
	private List<String> itemCells(KitType slot, SlotInfo info)
	{
		return ImmutableList.of(
			slot.name().toLowerCase(),
			info.isNothing() ? "0" : String.valueOf(info.getItemId()),
			info.getHidden().stream().sorted()
				.map(s -> s.name().toLowerCase()).
				collect(Collectors.joining(","))
		);
	}

	// slot, kitId, name
	private List<String> kitCells(KitType slot, Integer kitId)
	{
		Kit kit = KitUtil.KIT_ID_TO_KIT.get(kitId);
		return ImmutableList.of(
			slot.name().toLowerCase(),
			String.valueOf(kitId),
			kit != null ? kit.getDisplayName().toLowerCase() : ""
		);
	}

	// colorType, colorId, name
	private List<String> colorCells(ColorType type, Colorable value)
	{
		return ImmutableList.of(
			type.getDisplayName().toLowerCase(),
			String.valueOf(value.getColorId(type)),
			value.getDisplayName().toLowerCase()
		);
	}

	private void addNewlineAndTitle(String title)
	{
		if (!panelComponent.getChildren().isEmpty())
		{
			panelComponent.getChildren().add(MonospaceTitleComponent.builder()
				.text(" ")
				.build());
		}
		panelComponent.getChildren().add(MonospaceTitleComponent.builder()
			.text(title)
			.build());
	}
}
