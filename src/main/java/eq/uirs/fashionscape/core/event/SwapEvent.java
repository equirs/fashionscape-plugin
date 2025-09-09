package eq.uirs.fashionscape.core.event;

public abstract class SwapEvent
{
	public String getKey()
	{
		return this.getClass().getName();
	}
}
