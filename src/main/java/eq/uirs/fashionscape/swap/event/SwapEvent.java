package eq.uirs.fashionscape.swap.event;

public abstract class SwapEvent
{
	public String getKey()
	{
		return this.getClass().getName();
	}
}
