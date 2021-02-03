package eq.uirs.fashionscape.swap;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// observable wrapper for user's undo / redo history
@RequiredArgsConstructor
@Slf4j
class SnapshotQueues
{
	private final Function<Snapshot, Snapshot> snapshotRestore;
	// max number of snapshots to hold in each queue
	private static final int MAX_SNAPSHOTS = 10;

	private final LinkedList<Snapshot> undoSnapshots = new LinkedList<>();
	private final List<Consumer<Integer>> undoCountListeners = new LinkedList<>();

	private final LinkedList<Snapshot> redoSnapshots = new LinkedList<>();
	private final List<Consumer<Integer>> redoCountListeners = new LinkedList<>();

	void clear()
	{
		undoSnapshots.clear();
		redoSnapshots.clear();
	}

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
		if (undoSnapshots.isEmpty())
		{
			return;
		}
		Snapshot last = undoSnapshots.removeLast();
		undoCountListeners.forEach(listener -> listener.accept(undoSnapshots.size()));
		Snapshot redo = snapshotRestore.apply(last);
		addRedoSnapshot(redo);
	}

	void redoLast()
	{
		if (redoSnapshots.isEmpty())
		{
			return;
		}
		Snapshot last = redoSnapshots.removeLast();
		redoCountListeners.forEach(listener -> listener.accept(redoSnapshots.size()));
		Snapshot restore = snapshotRestore.apply(last);
		addUndoSnapshot(restore);
	}

	void appendToUndo(Snapshot snapshot)
	{
		if (snapshot == null || snapshot.isEmpty())
		{
			return;
		}
		addUndoSnapshot(snapshot);
		redoSnapshots.clear();
		redoCountListeners.forEach(listener -> listener.accept(0));
	}

	int undoSize()
	{
		return undoSnapshots.size();
	}

	int redoSize()
	{
		return redoSnapshots.size();
	}

	private void addUndoSnapshot(@Nullable Snapshot snapshot)
	{
		if (snapshot == null)
		{
			return;
		}
		Snapshot lastUndo = undoSnapshots.peekLast();
		if (lastUndo != null && snapshot.getChangedEquipmentIds().equals(lastUndo.getChangedEquipmentIds()))
		{
			return;
		}
		undoSnapshots.add(snapshot);
		while (undoSnapshots.size() > MAX_SNAPSHOTS)
		{
			undoSnapshots.removeFirst();
		}
		undoCountListeners.forEach(listener -> listener.accept(undoSnapshots.size()));
	}

	private void addRedoSnapshot(@Nullable Snapshot snapshot)
	{
		if (snapshot == null)
		{
			return;
		}
		Snapshot lastRedo = redoSnapshots.peekLast();
		if (lastRedo != null && snapshot.getChangedEquipmentIds().equals(lastRedo.getChangedEquipmentIds()))
		{
			return;
		}
		redoSnapshots.add(snapshot);
		while (redoSnapshots.size() > MAX_SNAPSHOTS)
		{
			redoSnapshots.removeFirst();
		}
		redoCountListeners.forEach(listener -> listener.accept(redoSnapshots.size()));
	}

}

