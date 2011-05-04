package com.sun.c1x.asm;

/**
 * A collection of static utility functions that check ranges of numbers.
 */
public class NumUtil {
	public static boolean isShiftCount(int x) {
		return 0 <= x && x < 32;
	}

	/**
	 * Determines if a given {@code int} value is the range of unsigned byte
	 * values.
	 */
	public static boolean isUByte(int x) {
		return (x & 0xff) == x;
	}

	/**
	 * Determines if a given {@code int} value is the range of signed byte
	 * values.
	 */
	public static boolean isByte(int x) {
		return (byte) x == x;
	}

	/**
	 * Determines if a given {@code long} value is the range of unsigned byte
	 * values.
	 */
	public static boolean isUByte(long x) {
		return (x & 0xffL) == x;
	}

	/**
	 * Determines if a given {@code long} value is the range of signed byte
	 * values.
	 */
	public static boolean isByte(long l) {
		return (byte) l == l;
	}

	/**
	 * Determines if a given {@code long} value is the range of unsigned int
	 * values.
	 */
	public static boolean isUInt(long x) {
		return (x & 0xffffffffL) == x;
	}

	/**
	 * Determines if a given {@code long} value is the range of signed int
	 * values.
	 */
	public static boolean isInt(long l) {
		return (int) l == l;
	}

	/**
	 * Determines if a given {@code int} value is the range of signed short
	 * values.
	 */
	public static boolean isShort(int x) {
		return (short) x == x;
	}

	public static boolean is32bit(long x) {
		return -0x80000000L <= x && x < 0x80000000L;
	}

	public static short safeToShort(int v) {
		assert isShort(v);
		return (short) v;
	}
}
