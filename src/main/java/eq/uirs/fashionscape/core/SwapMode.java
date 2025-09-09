package eq.uirs.fashionscape.core;

public enum SwapMode
{
	/**
	 * Swapping to a "virtual" item that will show up in the side panel
	 */
	SAVE,
	/**
	 * Indicates that whatever is being sent is not "virtual", so it should clear the panel UI
	 */
	REVERT,
	/**
	 * A temporary preview where the item will be swapped but it won't be saved to the panel
	 */
	PREVIEW
}
