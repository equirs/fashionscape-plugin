package eq.uirs.fashionscape.panel;

import javax.annotation.Nullable;
import net.runelite.api.ItemComposition;
import net.runelite.api.IterableHashTable;
import net.runelite.api.Node;

/**
 * ItemComposition representing "nothing". Only to be displayed in search results.
 */
class NothingItemComposition implements ItemComposition
{
	public static final String NAME = "Nothing";
	public static final int ID = -1;

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getMembersName()
	{
		return NAME;
	}

	@Override
	public void setName(String name)
	{

	}

	@Override
	public int getId()
	{
		return ID;
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
	public int getInventoryModel()
	{
		return 0;
	}

	@Override
	public void setInventoryModel(int model)
	{

	}

	@Nullable
	@Override
	public short[] getColorToReplace()
	{
		return new short[0];
	}

	@Override
	public void setColorToReplace(short[] colorsToReplace)
	{

	}

	@Override
	public short[] getColorToReplaceWith()
	{
		return new short[0];
	}

	@Override
	public void setColorToReplaceWith(short[] colorToReplaceWith)
	{

	}

	@Nullable
	@Override
	public short[] getTextureToReplace()
	{
		return new short[0];
	}

	@Override
	public void setTextureToReplace(short[] textureToFind)
	{

	}

	@Override
	public short[] getTextureToReplaceWith()
	{
		return new short[0];
	}

	@Override
	public void setTextureToReplaceWith(short[] textureToReplaceWith)
	{

	}

	@Override
	public int getXan2d()
	{
		return 0;
	}

	@Override
	public int getYan2d()
	{
		return 0;
	}

	@Override
	public int getZan2d()
	{
		return 0;
	}

	@Override
	public void setXan2d(int angle)
	{

	}

	@Override
	public void setYan2d(int angle)
	{

	}

	@Override
	public void setZan2d(int angle)
	{

	}

	@Override
	public int getAmbient()
	{
		return 0;
	}

	@Override
	public int getContrast()
	{
		return 0;
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
