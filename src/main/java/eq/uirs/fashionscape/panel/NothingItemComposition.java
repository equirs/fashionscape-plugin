package eq.uirs.fashionscape.panel;

import net.runelite.api.ItemComposition;
import net.runelite.api.IterableHashTable;
import net.runelite.api.Node;

public class NothingItemComposition implements ItemComposition
{
	@Override
	public String getName()
	{
		return "Nothing";
	}

	@Override
	public int getId()
	{
		return -1;
	}

	@Override
	public int getNote()
	{
		return 0;
	}

	@Override
	public int getLinkedNoteId()
	{
		return 0;
	}

	@Override
	public int getPlaceholderId()
	{
		return 0;
	}

	@Override
	public int getPlaceholderTemplateId()
	{
		return 0;
	}

	@Override
	public int getPrice()
	{
		return 0;
	}

	@Override
	public int getHaPrice()
	{
		return 0;
	}

	@Override
	public boolean isMembers()
	{
		return false;
	}

	@Override
	public boolean isStackable()
	{
		return false;
	}

	@Override
	public boolean isTradeable()
	{
		return false;
	}

	@Override
	public String[] getInventoryActions()
	{
		return new String[0];
	}

	@Override
	public int getShiftClickActionIndex()
	{
		return 0;
	}

	@Override
	public void setShiftClickActionIndex(int i)
	{
	}

	@Override
	public void resetShiftClickActionIndex()
	{
	}

	@Override
	public int getInventoryModel()
	{
		return 0;
	}

	@Override
	public short[] getColorToReplaceWith()
	{
		return new short[0];
	}

	@Override
	public short[] getTextureToReplaceWith()
	{
		return new short[0];
	}

	@Override
	public IterableHashTable<Node> getParams()
	{
		return null;
	}

	@Override
	public void setParams(IterableHashTable<Node> params)
	{
	}

	@Override
	public int getIntValue(int paramID)
	{
		return 0;
	}

	@Override
	public void setValue(int paramID, int value)
	{
	}

	@Override
	public String getStringValue(int paramID)
	{
		return null;
	}

	@Override
	public void setValue(int paramID, String value)
	{
	}
}
