/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.solrudev.ackpine.compress.utils;

import java.lang.reflect.Array;

// from org.apache.commons.lang3.ArrayUtils in commons-lang
/**
 * Operations on arrays, primitive arrays (like {@code int[]}) and
 * primitive wrapper arrays (like {@code Integer[]}).
 * <p>
 * This class tries to handle {@code null} input gracefully.
 * An exception will not be thrown for a {@code null}
 * array input. However, an Object array that contains a {@code null}
 * element may throw an exception. Each method documents its behavior.
 * </p>
 * <p>
 * #ThreadSafe#
 * </p>
 * @since 2.0
 */
public class ArrayUtils {

	/**
	 * Gets the length of the specified array.
	 * This method can deal with {@link Object} arrays and with primitive arrays.
	 * <p>
	 * If the input array is {@code null}, {@code 0} is returned.
	 * </p>
	 * <pre>
	 * ArrayUtils.getLength(null)            = 0
	 * ArrayUtils.getLength([])              = 0
	 * ArrayUtils.getLength([null])          = 1
	 * ArrayUtils.getLength([true, false])   = 2
	 * ArrayUtils.getLength([1, 2, 3])       = 3
	 * ArrayUtils.getLength(["a", "b", "c"]) = 3
	 * </pre>
	 *
	 * @param array  the array to retrieve the length from, may be {@code null}.
	 * @return The length of the array, or {@code 0} if the array is {@code null}
	 * @throws IllegalArgumentException if the object argument is not an array.
	 * @since 2.1
	 */
	public static int getLength(final Object array) {
		return array != null ? Array.getLength(array) : 0;
	}

	/**
	 * Checks if an array is empty or {@code null}.
	 *
	 * @param array the array to test
	 * @return {@code true} if the array is empty or {@code null}
	 */
	private static boolean isArrayEmpty(final Object array) {
		return getLength(array) == 0;
	}

	/**
	 * Tests whether an array of primitive bytes is empty or {@code null}.
	 *
	 * @param array  the array to test
	 * @return {@code true} if the array is empty or {@code null}
	 * @since 2.1
	 */
	public static boolean isEmpty(final byte[] array) {
		return isArrayEmpty(array);
	}
}
