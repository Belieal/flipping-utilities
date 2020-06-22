package com.flippingutilities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ModelUtilities
{
	/**
	 * Partition a list of items into n sublists based on n conditions passed in. Perhaps this should be a static method?
	 * The first condition puts items that meet its criteria in the first arraylist in the sublists array, the nth
	 * conditions puts the items in the nth arraylist in the sublists array.
	 *
	 * @param items      to partition into sub lists
	 * @param conditions conditions to partition on
	 * @return
	 */
	public static <T> List<T>[] partition(List<T> items, Predicate<T>... conditions)
	{
		List<T>[] subLists = new ArrayList[conditions.length];

		IntStream.range(0, subLists.length).forEach(i -> subLists[i] = new ArrayList<>());

		for (T item : items)
		{
			for (int i = 0; i < conditions.length; i++)
			{
				if (conditions[i].test(item))
				{
					subLists[i].add(item);
				}
			}
		}
		return subLists;
	}
}
