package eq.uirs.fashionscape.core.utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ListUtil
{
	/**
	 * Takes n random elements from the given list and returns them as a sublist.
	 * Returns the original list if n >= list size.
	 * Note that this function rearranges the list argument's contents.
	 * Implementation is based on the Fisher-Yates shuffle algorithm.
	 */
	public static <T> List<T> takeRandomSample(List<T> list, int n, Random random)
	{
		int size = list.size();
		if (n >= size)
		{
			return list;
		}
		for (int i = size - 1; i >= size - n; i--)
		{
			Collections.swap(list, i, random.nextInt(i + 1));
		}
		return list.subList(size - n, size);
	}
}
