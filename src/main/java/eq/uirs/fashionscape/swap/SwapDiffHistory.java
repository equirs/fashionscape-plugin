package eq.uirs.fashionscape.swap;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Observable wrapper for user's undo / redo history
 */
@RequiredArgsConstructor
@Slf4j
class SwapDiffHistory
{
	private final Function<SwapDiff, SwapDiff> restoration;
	private static final int MAX_LIST_SIZE = 10;

	private final LinkedList<SwapDiff> undoSwapDiffs = new LinkedList<>();
	private final List<Consumer<Integer>> undoCountListeners = new LinkedList<>();

	private final LinkedList<SwapDiff> redoSwapDiffs = new LinkedList<>();
	private final List<Consumer<Integer>> redoCountListeners = new LinkedList<>();

	void addUndoQueueChangeListener(Consumer<Integer> listener)
	{
		undoCountListeners.add(listener);
	}

	void addRedoQueueChangeListener(Consumer<Integer> listener)
	{
		redoCountListeners.add(listener);
	}

	void removeListeners()
	{
		undoCountListeners.clear();
		redoCountListeners.clear();
	}

	void undoLast()
	{
		if (undoSwapDiffs.isEmpty())
		{
			return;
		}
		SwapDiff last = undoSwapDiffs.removeLast();
		undoCountListeners.forEach(listener -> listener.accept(undoSwapDiffs.size()));
		SwapDiff redo = restoration.apply(last);
		addRedoDiff(redo);
	}

	void redoLast()
	{
		if (redoSwapDiffs.isEmpty())
		{
			return;
		}
		SwapDiff last = redoSwapDiffs.removeLast();
		redoCountListeners.forEach(listener -> listener.accept(redoSwapDiffs.size()));
		SwapDiff restore = restoration.apply(last);
		addUndoDiff(restore);
	}

	void appendToUndo(SwapDiff swapDiff)
	{
		if (swapDiff.isBlank())
		{
			return;
		}
		addUndoDiff(swapDiff);
		redoSwapDiffs.clear();
		redoCountListeners.forEach(listener -> listener.accept(0));
	}

	int undoSize()
	{
		return undoSwapDiffs.size();
	}

	int redoSize()
	{
		return redoSwapDiffs.size();
	}

	private void addUndoDiff(SwapDiff swapDiff)
	{
		if (swapDiff.isBlank())
		{
			return;
		}
		SwapDiff lastUndo = undoSwapDiffs.peekLast();
		if (swapDiff.equals(lastUndo))
		{
			return;
		}
		undoSwapDiffs.add(swapDiff);
		while (undoSwapDiffs.size() > MAX_LIST_SIZE)
		{
			undoSwapDiffs.removeFirst();
		}
		undoCountListeners.forEach(listener -> listener.accept(undoSwapDiffs.size()));
	}

	private void addRedoDiff(SwapDiff swapDiff)
	{
		if (swapDiff.isBlank())
		{
			return;
		}
		SwapDiff lastRedo = redoSwapDiffs.peekLast();
		if (swapDiff.equals(lastRedo))
		{
			return;
		}
		redoSwapDiffs.add(swapDiff);
		while (redoSwapDiffs.size() > MAX_LIST_SIZE)
		{
			redoSwapDiffs.removeFirst();
		}
		redoCountListeners.forEach(listener -> listener.accept(redoSwapDiffs.size()));
	}

}

