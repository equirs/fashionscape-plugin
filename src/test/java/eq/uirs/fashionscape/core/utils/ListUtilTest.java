package eq.uirs.fashionscape.core.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ListUtilTest
{
	private List<Integer> range(int n)
	{
		return IntStream.range(0, n).boxed().collect(Collectors.toCollection(ArrayList::new));
	}

	@Test
	void wholeListSampleReturnsOriginal()
	{
		List<Integer> list = range(5);
		assertSame(list, ListUtil.takeRandomSample(list, 5, new Random(0)));
		assertSame(list, ListUtil.takeRandomSample(list, 10, new Random(0)));
	}

	@Test
	void sampleNothing()
	{
		assertTrue(ListUtil.takeRandomSample(range(5), 0, new Random(0)).isEmpty());
	}

	@Test
	void takeSample()
	{
		List<Integer> list = range(10);
		List<Integer> sample = ListUtil.takeRandomSample(list, 4, new Random(0));
		assertEquals(4, sample.size());
		// should sample without replacement
		assertEquals(4, new HashSet<>(sample).size());
		// every element came from the source range
		assertTrue(list.containsAll(sample));
	}

	@Test
	void deterministicSample()
	{
		List<Integer> a = ListUtil.takeRandomSample(range(20), 6, new Random(1));
		List<Integer> b = ListUtil.takeRandomSample(range(20), 6, new Random(1));
		assertEquals(a, b);
	}
}
