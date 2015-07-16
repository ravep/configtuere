package com.unit16.r.onion.messaging;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.unit16.r.onion.messaging.MessageScheme;
import com.unit16.r.onion.messaging.MessageSchemeASM;
import com.unit16.r.onion.messaging.Source;

public class MessageSchemeASMTest {

	public static class MSGA {
		final int a;
		MSGA(int a_) {
			a = a_;
		}		
		@Override
		public String toString() {
			return String.format("MSGA[a=%d]", a);
		}
	}
	
	public static class MSGB {
		final int a;
		MSGB(int a_) {
			a = a_;
		}
		@Override
		public String toString() {
			return String.format("MSGB[a=%d]", a);
		}		
	}

	static public class RECP {

		
		int valA, valB;
		
		RECP(int ia, int ib) {
			valA = ia;
			valB = ib;
		}
		
		public void handle(MSGA a) {
			valA += a.a;
			
		}
		
		public void handle(MSGB b) {
			valB += b.a;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d)", valA, valB);
		}
		
	}
	
	void check(RECP d, int a, int b) {
		assertEquals(a, d.valA);
		assertEquals(b, d.valB);
	}
	
	@Test
	public void test() {
		
		Source<MSGA> ma = new Source<>(MessageSchemeASMTest.class, MSGA.class);
		Source<MSGB> mb = new Source<>(MessageSchemeASMTest.class, MSGB.class);
		
		RECP d1 = new RECP(0,1);
		RECP d2 = new RECP(5,6);
		RECP d3 = new RECP(10,11);
		
		MessageScheme scheme = new MessageSchemeASM("booh");
		
		scheme.connect(ma,  d1);
		scheme.connect(ma,  d2);
		scheme.connect(mb,  d2);
		scheme.connect(mb,  d3);
		
		check(d1, 0, 1);
		check(d2, 5, 6);
		check(d3, 10, 11);
		
		System.out.println(d1 + " " + d2 + " " + d3);
		ma.tell(new MSGA(3));
		System.out.println(d1 + " " + d2 + " " + d3);
		
		check(d1, 3, 1);
		check(d2, 8, 6);
		check(d3, 10, 11);

		mb.tell(new MSGB(-2));
		
		System.out.println(d1 + " " + d2 + " " + d3);

		check(d1, 3, 1);
		check(d2, 8, 4);
		check(d3, 10, 9);

		
		
	}

}
