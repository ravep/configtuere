package com.unit16.r.onion.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLatency {

	@Test
	public void test() {
		Latency l = new Latency.I("test", 10);
		
		for(int ii = 0; ii < 10; ii++) {
			l.add(10);
		}
		
		assertEquals(10, l.avg(), 1.0e-6);
		assertEquals(10, l.min(), 1.0e-6);
		assertEquals(10, l.max(), 1.0e-6);
		
		for(int ii = 11; ii < 20; ii++) {
			l.add(10);
		}
		
		for(int ii = 10; ii <= 20; ii++) {
			l.add(ii);
		}
		
		assertEquals(15, l.avg(), 1.0e-6);
		assertEquals(10, l.min(), 1.0e-6);
		assertEquals(20, l.max(), 1.0e-6);

		// adding twenty a couple of times
		l.add(20);
		assertEquals(10, l.min(), 1.0e-6);
		assertEquals(20, l.max(), 1.0e-6);
		l.add(20);
		l.add(20);		
		assertEquals(11, l.min(), 1.0e-6);
		assertEquals(20, l.max(), 1.0e-6);
			
	}

}
