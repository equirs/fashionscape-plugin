package eq.uirs.fashionscape.core;

import com.google.inject.Inject;
import eq.uirs.fashionscape.base.BaseLayersTest;
import eq.uirs.fashionscape.data.color.ColorType;
import net.runelite.api.kit.KitType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class HistoryTest extends BaseLayersTest
{
	@Inject
	private History history;

	private Diff colorDiff(int from, int to)
	{
		return Diff.ofColor(ColorType.HAIR, from, to);
	}

	@Test
	void emptyAppendIgnored()
	{
		history.append(Diff.empty());
		assertEquals(0, history.undoSize());
	}

	@Test
	void duplicateAppendIgnored()
	{
		history.append(colorDiff(0, 1));
		history.append(colorDiff(0, 1));
		assertEquals(1, history.undoSize());
	}

	@Test
	void undoingThenAppending()
	{
		history.append(colorDiff(0, 1));
		history.undo();
		assertEquals(0, history.undoSize());
		assertEquals(1, history.redoSize());
		// appending should clear the redo queue
		history.append(colorDiff(2, 3));
		assertEquals(1, history.undoSize());
		assertEquals(0, history.redoSize());
	}

	@Test
	void historyReachesMaxSize()
	{
		for (int i = 0; i < 12; i++)
		{
			history.append(colorDiff(i, i + 1));
		}
		assertEquals(10, history.undoSize());
	}

	@Test
	void modifyingHistoryChangesLayers()
	{
		Diff diff = layers.set(KitType.LEGS, TestData.mimeLegs, false);
		history.append(diff);
		assertEquals(TestData.mimeLegs, layers.getVirtualModels().getItems().get(KitType.LEGS));

		history.undo();
		assertNull(layers.getVirtualModels().getItems().get(KitType.LEGS));
		assertEquals(0, history.undoSize());
		assertEquals(1, history.redoSize());

		history.redo();
		assertEquals(TestData.mimeLegs, layers.getVirtualModels().getItems().get(KitType.LEGS));
		assertEquals(1, history.undoSize());
		assertEquals(0, history.redoSize());
	}
}
