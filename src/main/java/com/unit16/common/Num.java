package com.unit16.common;

import static com.google.common.base.Preconditions.checkArgument;

public class Num {

	public static String readable(double v) {
		
		final double abs = Math.abs(v);
		final String[] kk = {"B", "M", "K", "", ""};
		final double[] sz = {1e9, 1e6, 1e3, 1, 1e-3};
		final double[] div = {1e9, 1e6, 1e3, 1, 1};
		final int[] dgt = {1, 1, 1, 2, 3};
		
		for(int ii = 0; ii < kk.length; ii++) {
			if(abs > sz[ii]) {
				return String.format("%s%." + dgt[ii] + "f%s", 
					v < 0 ? "-" : "", abs / div[ii], kk[ii]);
			}
		}
		return String.format("%g", v);
		
	}
	
	public static int toInt(long l) {		
		checkArgument(l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE, "cast fail: long %s doesn't fit into int", l);
		return (int)l;
	}
	
	public static long isLong(double value) throws Exception {
		if(Math.abs(Math.round(value) - value) < 1e-8 * value && Math.abs(value) < Long.MAX_VALUE) {
			return (long)Math.round(value);
		}
		throw new Exception("Value is not long " + value);
	}
	
	public static int isInt(double value) throws Exception {
		if(Math.abs(Math.round(value) - value) < 1e-8 * value && Math.abs(value) < Integer.MAX_VALUE) {
			return (int)Math.round(value);
		}
		throw new Exception("Value is not integer " + value);
	}

	public static int safeDivision(int value, int div) {
		int ret = value / div;
		if(value != ret * div) {
			throw new IllegalArgumentException(String.format(
				"Truncating int division when dividing %d / %d", value, div));
		}
		return ret;
	}
	
	public static long safeDivision(long value, int div) {
		long ret = value / div;
		if(value != ret * div) {
			throw new IllegalArgumentException(String.format(
				"Truncating long division when dividing %d / %d", value, div));
		}
		return ret;
	}

	
}
