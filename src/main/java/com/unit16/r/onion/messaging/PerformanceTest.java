package com.unit16.r.onion.messaging;

import java.util.ArrayList;
import java.util.List;

public class PerformanceTest {

	public static class M {
		long a;
	}
	
	public static class A {
		
		long d;
		A(long i) {
			d = i;
		}
		
		public void handle(M m) {
			d += m.a;
		}
	}
	
	public PerformanceTest(MessageScheme s, int loops, int loopSize, int csize) {
		List<A> dests = new ArrayList<>();
		Source<M> src = new Source<>("test", M.class);
		for(int ii = 0; ii < csize; ii++) {
			A a;
			dests.add(a = new A(ii));
			s.connect(src, a);
		}
		M m = new M();
		for(int ll = 0; ll < loops; ll++) {
			long seed = Math.round(Integer.MAX_VALUE * Math.random());
			long l1 = System.nanoTime();
			long sig = 1;
			m.a = ll;
			for(int ii = 0; ii < loopSize; ii++) {
				m.a += sig * (ll + seed);	
				src.tell(m);
				sig = - sig;
			}
			long l2 = System.nanoTime();
			long v3 = m.a;
			m.a = ll;
			sig = 1;
			for(int ii = 0; ii < loopSize; ii++) {
				m.a += sig * (ll + seed);
				sig = - sig;
			}	
			if(m.a != v3) {
				System.out.println("NJET");
			}
			long l3 = System.nanoTime();
			System.out.println(String.format("iteration %d, %d calls in %g seconds (%g nanos per call), math/(math+disp): %g",
				ll, loopSize, (l2 - l1) / 1e9, (l2 - l1) / (double)loopSize, (l3 - l2) / (double)(l2 - l1))); 	
			if(v3 > Long.MAX_VALUE) {
				System.out.println("booh");
			}
		}
		StringBuilder b = new StringBuilder();
		for(A a : dests) {
			b.append(a.d);
		}
	}

	public static void main(String[] args) {
		new PerformanceTest(new MessageSchemeASM("test"), 5, 30000, 5);
	}

}
