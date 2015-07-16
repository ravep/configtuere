package com.unit16.common;

import java.util.Arrays;
import java.util.List;

public class Str {

	public static <T> String join(List<T> v) {
		return join(", ", Arrays.asList(v));
	}
	
	public static <T> String join(T[] v) {
		return join(", ",v);
	}

	public static <T> String join(String delim, T[] v) {
		return join(delim, Arrays.asList(v));
	}
	
	public static <T> String join(String delim, List<T> v) {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for(T t : v) {
			if(!first) {
				b.append(delim);
			}
			b.append(t);
			first = false;
		}
		return b.toString();
	}
	
}
