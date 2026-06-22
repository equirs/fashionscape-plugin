package eq.uirs.fashionscape.core.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

// a.k.a. undo/redo queue change
@EqualsAndHashCode(callSuper = false)
@Value
public class HistoryChanged extends FashionscapeEvent
{
	boolean isUndo; // false if redo
	int newSize;
}
