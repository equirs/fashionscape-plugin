package eq.uirs.fashionscape.swap;

public abstract class SwapEvent
{
	String getKey()
	{
		return this.getClass().getName();
	}
}
