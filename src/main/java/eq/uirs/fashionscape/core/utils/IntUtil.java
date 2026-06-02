package eq.uirs.fashionscape.core.utils;

import javax.annotation.Nullable;

public class IntUtil
{
	@Nullable
	public static Integer safeParse(String value)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
