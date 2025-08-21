/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ru.solrudev.ackpine.compress.archivers.zip;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class that represents an eight byte integer with conversion rules for the little-endian byte order of ZIP files.
 *
 * @Immutable
 * @since 1.2
 */
public final class ZipEightByteInteger implements Serializable {

	/**
	 *  The number of bytes used to represent an instance in binary form.
	 */
	static final int BYTES = 8;

	private static final long serialVersionUID = 1L;

	/**
	 * Constant for a value of zero.
	 */
	public static final ZipEightByteInteger ZERO = new ZipEightByteInteger(0);

	private static final BigInteger HIGHEST_BIT = BigInteger.ONE.shiftLeft(63);

	/**
	 * Gets value as eight bytes in big-endian byte order.
	 *
	 * @param value the value to convert.
	 * @return value as eight bytes in big-endian byte order.
	 */
	public static byte[] getBytes(final BigInteger value) {
		return getBytes(value.longValue());
	}

	/**
	 * Gets value as eight bytes in big-endian byte order.
	 *
	 * @param value the value to convert.
	 * @return value as eight bytes in big-endian byte order.
	 */
	public static byte[] getBytes(final long value) {
		return ByteBuffer.allocate(BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
	}

	/**
	 * Gets the value as a Java long from an eight-byte array.
	 *
	 * @param bytes the array of bytes.
	 * @return the corresponding Java long value.
	 */
	public static long getLongValue(final byte[] bytes) {
		return getLongValue(bytes, 0);
	}

	/**
	 * Gets the value as a Java long from eight bytes starting at given array offset.
	 *
	 * @param bytes  the array of bytes.
	 * @param offset the offset to start.
	 * @return the corresponding Java long value.
	 */
	public static long getLongValue(final byte[] bytes, final int offset) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong(offset);
	}

	/**
	 * Gets the value as a Java long from an eight-byte array.
	 *
	 * @param bytes the array of bytes.
	 * @return the corresponding Java BigInteger value.
	 */
	public static BigInteger getValue(final byte[] bytes) {
		return getValue(bytes, 0);
	}

	/**
	 * Gets the value as a Java BigInteger from eight bytes starting at given array offset.
	 *
	 * @param bytes  the array of bytes.
	 * @param offset the offset to start.
	 * @return the corresponding Java BigInteger value.
	 */
	public static BigInteger getValue(final byte[] bytes, final int offset) {
		return toUnsignedBigInteger(getLongValue(bytes, offset));
	}

	/**
	 * package private for tests only.
	 */
	static BigInteger toUnsignedBigInteger(final long value) {
		if (value >= 0L) {
			return BigInteger.valueOf(value);
		}
		return BigInteger.valueOf(value & Long.MAX_VALUE).add(HIGHEST_BIT);
	}

	/**
	 * The value is treated as unsigned.
	 */
	private final long value;

	/**
	 * Constructs a new instance from a number.
	 *
	 * @param value the BigInteger to store as a ZipEightByteInteger
	 */
	public ZipEightByteInteger(final BigInteger value) {
		this.value = value.longValue();
	}

	/**
	 * Constructs a new instance from bytes.
	 *
	 * @param bytes the bytes to store as a ZipEightByteInteger.
	 */
	public ZipEightByteInteger(final byte[] bytes) {
		this(bytes, 0);
	}

	/**
	 * Constructs a new instance from the eight bytes starting at offset.
	 *
	 * @param bytes  the bytes to store as a ZipEightByteInteger.
	 * @param offset the offset to start.
	 */
	public ZipEightByteInteger(final byte[] bytes, final int offset) {
		this.value = getLongValue(bytes, offset);
	}

	/**
	 * Constructs a new instance from a number.
	 *
	 * @param value the long to store as a ZipEightByteInteger.
	 */
	public ZipEightByteInteger(final long value) {
		this.value = value;
	}

	/**
	 * Override to make two instances with same value equal.
	 *
	 * @param o an object to compare.
	 * @return true if the objects are equal.
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ZipEightByteInteger)) {
			return false;
		}
		return value == ((ZipEightByteInteger) o).value;
	}

	/**
	 * Gets value as eight bytes in big-endian byte order.
	 *
	 * @return value as eight bytes in big-endian order.
	 */
	public byte[] getBytes() {
		return getBytes(value);
	}

	/**
	 * Gets value as Java long.
	 *
	 * @return value as a long.
	 */
	public long getLongValue() {
		return value;
	}

	/**
	 * Gets value as Java BigInteger.
	 *
	 * @return value as a BigInteger.
	 */
	public BigInteger getValue() {
		return toUnsignedBigInteger(value);
	}

	/**
	 * Override to make two instances with same value equal.
	 *
	 * @return the hash code of the value stored in the ZipEightByteInteger.
	 */
	@Override
	public int hashCode() {
		// from Long#hashCode()
		//noinspection UseHashCodeMethodInspection
		return (int)(value ^ (value >>> 32));
	}

	@Override
	public String toString() {
		// from Long#toUnsignedString()
		if (value >= 0) {
			return Long.toString(value, 10);
		}
		long quot = (value >>> 1) / 5;
		long rem = value - quot * 10;
		return Long.toString(quot) + rem;
//		return Long.toUnsignedString(value);
	}
}
