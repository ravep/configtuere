package com.unit16.common;

public class IdGenerator {

	private static int nextOrderId = 1;
	private static int nextRequestId = 1;
	
	public static void reset() {
		nextOrderId = 1;
		nextRequestId = 1;
	}

	public static int nextOrderId() {
		return nextOrderId++;
	}

	public static int nextRequestId() {
		return nextRequestId++;
	}
	
	
	

}
