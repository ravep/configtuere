package com.unit16.r.onion.messaging;

import org.junit.Test;

import com.unit16.r.onion.messaging.MessageScheme;
import com.unit16.r.onion.messaging.Source;

import static org.junit.Assert.*;

public class TestMessaging {

	public class Handle {
		
		String expect;
		
		Handle() {
			expect = null;
		}
		
		void expect(String exp) {
			if(expect != null) {
				fail("Expected " + expect + " but didnt get it");
			}
			expect = exp;
		}
		
		public void handle(String s) {
			if(expect == null) {
				fail("received " + s + " but didn't expect");
			}
			else if(!expect.equals(s)) {
				fail("received " + s + " but expected " + expect);
			}
			expect = null;
		}
		
	}
	
	@Test
	public void test() {
		MessageScheme s = new MessageScheme("");
		Source<String> source = new Source<>(getClass(), String.class);
		Handle dest = new Handle();
		Handle two = new Handle();
		String tst = "boooh";		
		s.connect(source, dest);
		dest.expect(tst);
		source.tell("boooh");
		dest.expect("boooh");
		two.expect("boooh");
		s.connect(source, two);
		source.tell("boooh");
		dest.expect("boooh");
		two.expect("boooh");
		
		s.connect(source, new Object() {
			@SuppressWarnings("unused")
			public void handle(String s) {
				System.out.println("BOOOH");
			}
		});
		source.tell("boooh");

		
		
	}
	
}
