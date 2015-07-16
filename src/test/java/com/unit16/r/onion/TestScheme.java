package com.unit16.r.onion;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.unit16.datastructures.Pair;
import com.unit16.r.onion.messaging.MessageScheme;
import com.unit16.r.onion.messaging.Source;
import com.unit16.r.onion.util.Clock;

public class TestScheme {
	
	public static class Ping extends Component {

		Source<Ping> obs;
		int value = 0;
		int increment;
		public Ping(int increment_, String n, Ring ring_) {
			super(n, ring_);
			obs = new Source<>(getClass(), Ping.class);
			increment = increment_;
		}

		public Source<Ping> observable() {
			return obs;
		}		
		
		@Override
		public void update() {
			if(Math.abs(value) < 10) {
				log.debug("update: {} is telling at {}", name(), value);
				obs.tell(this);
			} else {
				log.debug("update: {} is keeping mouth shut at {}", name(), value);
			}
		}

		public void handle(Ping obj) {
			value = obj.value + increment;
			log.debug("handle: Updated value to {}", value);
			schedule();
		}
		
	}
	
	public static class SuperPing extends Ping {
		
		private final Clock clock;
		private boolean first = true;
		
		public SuperPing(Clock clock_, int increment_, String n, Ring ring_) {
			super(increment_, n, ring_);
			clock = clock_;
		}

		public void handle(Ping obj) {
			clock.schedule(clock.gmtNanos() + 60_000_000, new Clock.Action() {
				@Override
				public long scheduledAction(long nanotime) {
					obs.tell(SuperPing.this);
					if(first) {
						first = false;
						return clock.gmtNanos() + 60_000_000;
					}
					return 0;
				}				
			});
		}

	}
	
	public static class Reciever {
		
		LinkedList<Pair<String,Integer>> incoming = new LinkedList<>();
		
		public void handle(Ping p) {
			incoming.add(Pair.I.pair(p.name(), p.value));
		}
		
	}
	
	private Reciever initPeeler(final Peeler.A runner) {
		
		final Ring inner = runner.appendRing("inner");
		final Ring outer = runner.appendRing("outer");
		runner.appendRing("last");
		runner.init();

		final Source<Ping> extsrc = new Source<>(TestScheme.class, Ping.class);
		final Reciever recv = new Reciever();
				
		new MessageScheme("test") {
			{
				final Ping a = new Ping(1, "inc1", inner);
				final Ping b = new Ping(2, "inc2", inner);
				final Ping c = new Ping(-4, "dec4", outer);	
				// ping d gets pings from clock (at the end)
				final Ping d = new Ping(0, "ping-on-clock", outer);
				final SuperPing s = new SuperPing(runner.clock(), 0, "super", inner);
				connect(a.observable(), b);
				connect(b.observable(), c);
				connect(c.observable(), a);
				for(Ping p : Arrays.asList(a,b,c,d,s)) {
					connect(p.observable(), recv);
				}
				// pings from 
				connect(d.observable(), s);
				a.schedule();
				runner.clock().schedule(runner.clock().gmtNanos() + 100_000_000,  new Clock.Action() {					
					boolean first = true;					
					@Override
					public long scheduledAction(long nanotime) {
						d.schedule();
						if(first) {
							first = false;
							return runner.clock().gmtNanos() + 100_000_000;
						}
						return 0;
					}
				});
				connect(extsrc, recv, runner);
				runner.clock().schedule(runner.clock().gmtNanos() + 120_000_000, new Clock.Action() {
					@Override
					public long scheduledAction(long nanotime) {
						extsrc.tell(a);
						return 0;
					}					
				});
			}
		};
		
		
		
		LoggerFactory.getLogger("tmp").debug("starting over");
		
		return recv;
	}
	
	@Test
	public void testSpinning() {
		
		Peeler.SpinningPeeler runner = new Peeler.SpinningPeeler("test");
		Reciever recv = initPeeler(runner);
		
		long start = System.currentTimeMillis();
		while(runner.atomic() && System.currentTimeMillis() - start < 500) {}
		
		evalPeelerTest(recv);
		
	}
	
	@Test
	public void testSimPeeler() {
		DateTime now = new DateTime();
		Peeler.SimPeeler runner = new Peeler.SimPeeler("test", now, now.plusSeconds(1));
		Reciever recv = initPeeler(runner);
		runner.run();
		evalPeelerTest(recv);
	}

	@Test
	public void testBackgroundPeeler() throws InterruptedException {
		Peeler.BackgroundPeeler runner = new Peeler.BackgroundPeeler("test");
		Reciever recv = initPeeler(runner);
		runner.start();
		Thread.sleep(500);
		runner.stop();
		evalPeelerTest(recv);
	}
	
	private void evalPeelerTest(Reciever recv) {
				
		assertEquals(32, recv.incoming.size());
		assertEquals(-6, recv.incoming.get(25).snd().intValue());
		
		String[] lastOnes = {"inc2", "ping-on-clock", "inc1", "super", "ping-on-clock", "super", "super"};
		for(int ii = 25; ii < recv.incoming.size(); ii++) {
			assertEquals(lastOnes[ii - 25], recv.incoming.get(ii).fst());
		}

	}

}
