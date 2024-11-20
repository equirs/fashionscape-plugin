package eq.uirs.fashionscape.core;

import eq.uirs.fashionscape.core.event.HistoryChanged;
import eq.uirs.fashionscape.core.layer.Layers;
import eq.uirs.fashionscape.core.layer.Locks;
import eq.uirs.fashionscape.data.color.ColorType;
import eq.uirs.fashionscape.data.kit.JawIcon;
import java.util.LinkedList;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.kit.KitType;

/**
 * Observable wrapper for user's undo / redo history of virtual models.
 */
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Singleton
@Slf4j
class History
{
	private static final int MAX_LIST_SIZE = 10;

	private final Layers layers;
	private final Locks locks;

	private final LinkedList<Diff> undoHistory = new LinkedList<>();
	private final LinkedList<Diff> redoHistory = new LinkedList<>();

	void undo()
	{
		if (undoHistory.isEmpty())
		{
			return;
		}
		Diff last = undoHistory.removeLast();
		onUndo();
		Diff redo = restore(last);
		addRedoDiff(redo);
	}

	void redo()
	{
		if (redoHistory.isEmpty())
		{
			return;
		}
		Diff last = redoHistory.removeLast();
		onRedo();
		Diff restore = restore(last);
		addUndoDiff(restore);
	}

	void append(Diff diff)
	{
		if (diff.isEmpty())
		{
			return;
		}
		addUndoDiff(diff);
		redoHistory.clear();
		onRedo();
	}

	int undoSize()
	{
		return undoHistory.size();
	}

	int redoSize()
	{
		return redoHistory.size();
	}

	private void addUndoDiff(Diff diff)
	{
		if (diff.isEmpty())
		{
			return;
		}
		Diff lastUndo = undoHistory.peekLast();
		if (diff.equals(lastUndo))
		{
			return;
		}
		undoHistory.add(diff);
		while (undoHistory.size() > MAX_LIST_SIZE)
		{
			undoHistory.removeFirst();
		}
		onUndo();
	}

	private void addRedoDiff(Diff diff)
	{
		if (diff.isEmpty())
		{
			return;
		}
		Diff lastRedo = redoHistory.peekLast();
		if (diff.equals(lastRedo))
		{
			return;
		}
		redoHistory.add(diff);
		while (redoHistory.size() > MAX_LIST_SIZE)
		{
			redoHistory.removeFirst();
		}
		onRedo();
	}

	private void onUndo()
	{
		Events.fire(new HistoryChanged(true, undoSize()));
	}

	private void onRedo()
	{
		Events.fire(new HistoryChanged(false, redoSize()));
	}

	private Diff restore(Diff diff)
	{
		Diff result = Diff.empty();
		for (KitType slot : KitType.values())
		{
			// outSlots may not contain this slot, but even if not, inSlots may have changed
			SlotInfo info = diff.outSlots.get(slot);
			// put whatever went out back if it's different from inSlot (and if locks allow)
			if (!Objects.equals(info, diff.inSlots.get(slot)) && locks.isAllowed(slot, info))
			{
				result = Diff.merge(layers.set(slot, info, false), result);
			}
		}
		for (ColorType type : ColorType.values())
		{
			Integer colorId = diff.outColors.get(type);
			// put the colorId back if it's different from inColor and if unlocked
			if (!Objects.equals(colorId, diff.inColors.get(type)) && !locks.getColor(type))
			{
				result = Diff.merge(layers.setColor(type, colorId, false), result);
			}
		}
		JawIcon icon = diff.outIcon;
		if (!locks.isIcon())
		{
			result = Diff.merge(layers.setIcon(icon, false), result);
		}
		return result;
	}
}

