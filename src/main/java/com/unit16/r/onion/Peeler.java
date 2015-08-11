package com.unit16.r.onion;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.unit16.r.onion.messaging.Dispatcher;
import com.unit16.r.onion.messaging.MessageQueue;
import com.unit16.r.onion.util.Clock;
import com.unit16.r.onion.util.RealTimeClock;
import com.unit16.r.onion.util.SimulationClock;
import com.unit16.r.onion.util.Yield;
import com.unit16.z.Pair;

public interface Peeler extends MessageQueue {
	
	public void ringScheduled(int idx);
	public Clock clock();
	public Yield yield();
	
	public abstract static class A implements Peeler {
		
		final ArrayList<Ring.I> rings; 
		
		private int currentRing;
		protected final Logger log;
		
		public A(String name_) {
			rings = new ArrayList<>();
			currentRing = 0;
			log = LoggerFactory.getLogger(Peeler.class);
		}
		
		public void init() {}
			
		public Ring appendRing(String name) {
			log.debug("Creating ring idx {}: {}", rings.size(), name);
			rings.add(new Ring.I(this, rings.size(), name));
			return rings.get(rings.size() - 1);
		}
		
		protected Component poll() {
			Ring.I cur = rings.get(currentRing);		
			if(cur.hasWork()) {
				return cur.poll();
			} else if(currentRing < rings.size() - 1) {
				currentRing++;
				return poll();	
			} else {
				return null;
			}
		}
		
		public void ringScheduled(int idx) {
			if(idx < currentRing) {
				currentRing = idx;
			}
		}
	}
	
	public static class SimPeeler extends A {

		private final Pair.Uniform<DateTime> times;
		private SimulationClock clock;
		private MessageQueue queue;
		
		public SimPeeler(String name_, DateTime start, DateTime stop) {
			super(name_);
			times = Pair.Uniform.uniform(start, stop);
		}
		
		@Override
		public void init() {
			Preconditions.checkArgument(clock == null, "already initialized");
			Preconditions.checkArgument(rings.size() > 0);
			Ring ring = rings.get(rings.size() - 1);
			clock = new SimulationClock(ring, times.fst(), times.snd());			
			queue = new MessageQueue.I(ring, clock);
		}

		@Override
		public Ring appendRing(String name) {
			Preconditions.checkArgument(clock == null, "can not add ring %s after initialization", name);
			return super.appendRing(name);
		}
		
		public SimulationClock clock() {
			return Preconditions.checkNotNull(clock, "uninitialized");
		}
		
		/** simulation running (ends when there is no message left) */
		public void run() {
			Component next = poll(); 
			while(next != null) {
				next.process();
				next = poll();
			}
		}

		@Override
		public Yield yield() {
			return clock;
		}

		@Override
		public void incoming(Dispatcher d, Object msg) {
			queue.incoming(d, msg);
		}
		
	}
	
	public static class SpinningPeeler extends A {

		private RealTimeClock clock;
		private Yield yield;
		private MessageQueue queue;
		
		public SpinningPeeler(String name_) {
			super(name_);
		}
		
		protected RealTimeClock create(Ring r, Yield y) {
			return new RealTimeClock(r, y);
		}
		
		public boolean atomic() {
			Component next = poll();
			if(next != null) {
				next.process();
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public void init() {
			Preconditions.checkArgument(clock == null, "already initialized");
			Preconditions.checkArgument(rings.size() > 0);
			Ring ring = rings.get(rings.size() - 1);
			yield = new Yield.I("coldYield", ring);
			clock = create(ring, yield);
			queue = new MessageQueue.I(ring, yield);
		}

		@Override
		public RealTimeClock clock() {
			return Preconditions.checkNotNull(clock, "uninitialized");
		}

		@Override
		public Yield yield() {
			return Preconditions.checkNotNull(yield, "uninitialized");
		}
				
		@Override
		public Ring appendRing(String name) {
			Preconditions.checkArgument(clock == null, "can not add ring %s after initialization", name);
			return super.appendRing(name);
		}

		@Override
		public void incoming(Dispatcher d, Object msg) {
			queue.incoming(d, msg);
		}		
		
	}
	
	public static class BackgroundPeeler extends A {
		
		private final Thread thread;
		private Thread timer; 
		private volatile boolean running = true;
		private volatile long volNextSchedule;
		private volatile boolean haveActions = false;
		private final LinkedBlockingQueue<Pair<Dispatcher, Object>> incoming = new LinkedBlockingQueue<>();
		private final Pair<Dispatcher, Object> clockEvent;
		private Clock clock;
		private Yield yield;
		private final Object monitor = new Object();

		public BackgroundPeeler(String name_) {
			super(name_);			
			clockEvent = Pair.I.pair(null, null);
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(running) {
						Component next = poll();
						if(next != null) {
							next.process();
						} else {
							try {
								Pair<Dispatcher, Object> msg = incoming.poll(500, TimeUnit.MILLISECONDS);
								if(msg != null) {
									if(msg.fst() != null) {
										msg.fst().dispatch(msg.snd());
									} else {
										// clock event
										clock.schedule();
									}
								}
							} catch (InterruptedException e) {}
						}
					}					
				}				
			});
			thread.setName(name_);
		}

		@Override
		public void init() {
			
			
			timer = new Thread(new Runnable() {

				@Override
				public void run() {
					while(running) {						
						long waiting = 1000;
						if(haveActions) {
							waiting = ((volNextSchedule - clock.gmtNanos()) / 1_000_000 - 1);
							if(waiting <= 0) {
								incoming.add(clockEvent);
								waiting = 1000;
							}
							waiting = Math.max(1, Math.min(1000,  waiting));
						}					
						synchronized(monitor) {
							try {							
								monitor.wait(waiting);
							} catch (InterruptedException e) {}
						}
						
					}
				}
				
			});
			timer.setDaemon(true);
			timer.setName("Timer");
			
			Ring ring = rings.get(rings.size() - 1);
			yield = new Yield.I("coldYield", ring);
			
			clock = new Clock(ring, false) {
				
				{
					volNextSchedule = Long.MAX_VALUE;
				}
				
				@Override
				public long gmtNanos() {
					return System.currentTimeMillis() * 1_000_000l;
				}
				
				@Override
				protected void reschedule() {
					volNextSchedule = nextScheduled;
					haveActions = actions.size() > 0;
					synchronized(monitor) {
						monitor.notify();
					}					
				}
			};
			
		}
		
		@Override
		public Clock clock() {
			return Preconditions.checkNotNull(clock, "uninitialized");
		}

		@Override
		public Yield yield() {
			return Preconditions.checkNotNull(yield, "uninitialized");
		}

		public void start() {			
			thread.start();	
			timer.start();
		}
		
		public void stop() {
			running = false;
			synchronized(monitor) {
				monitor.notify();
			}
			try {
				thread.join(2000);
			} catch(Exception e) {}
		}

		@Override
		public void incoming(Dispatcher d, Object msg) {
			incoming.add(Pair.I.pair(d, msg));
		}

		public void join() throws InterruptedException {
			thread.join();
		}
		
	}

	
	
	
}
