package eq.uirs.fashionscape.core.event;

public abstract class FashionscapeEvent
{
	public String getKey()
	{
		return this.getClass().getName();
	}
}
