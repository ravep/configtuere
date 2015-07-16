package com.unit16.r.onion.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.unit16.r.onion.Component;
import com.unit16.r.onion.Ring;

// TODO: change this to an interface (since people should not know that this is a component too!)
public abstract class Clock extends Component {
	
	public interface Action {
		//!< called at scheduled time. if return value > 0, then action will be rescheduled
		long scheduledAction(long nanotime);
	}
		
	public abstract long gmtNanos();
	
	public DateTime dateTime(DateTimeZone dz) {
		return new DateTime(TimeUnit.NANOSECONDS.toMillis(gmtNanos()), dz);
	}
	
	public DateTime dateTime() {
		 return dateTime(DateTimeZone.getDefault());
	}
	
	public static DateTime fromNanos(long nanos) {
        return new DateTime(TimeUnit.NANOSECONDS.toMillis(nanos)).withZone(DateTimeZone.UTC);
	}
	
	class ScheduledAction implements Comparable<ScheduledAction> {
		long actionIndex;	//!< action index is used to keep stuff that happens in the same moment within the order it was submitted
		long nanos;
		final Action action;
		ScheduledAction(long actionIndex_, long nanos_, Action action_) {
			action = action_;
			nanos = nanos_;
			actionIndex = actionIndex_;
		}
		@Override
		public int compareTo(ScheduledAction o) {
			if(nanos < o.nanos) {
				return -1;
			} else if(nanos > o.nanos) {
				return 1;
			} else if(actionIndex < o.actionIndex) {
				return -1;
			} else if(actionIndex > o.actionIndex) {
				return 1;
			}
			throw new RuntimeException("two events have the same actionIndex? " + this + " " + o);
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(getClass())
				.add("nanos", fromNanos(nanos))
				.add("nanoLong", nanos)
				.add("actionIndex", actionIndex)
				.add("action", action)				
				.toString();
		}
	}
	
	protected final PriorityQueue<ScheduledAction> actions = new PriorityQueue<>();
	protected long nextScheduled;
	long nextActionIndex;
	private final Latency latency;
	private final Latency bookToOrderLat;
	private long lastNanoTime;
	private long incoming;
	private long lastBookToOrderTime;
	private final boolean measureLatency;
	private int latencyWarnings = 10;
	private volatile long cycle = 0;
	private boolean ignoreCycleWarn = true;

	protected Clock(Ring ring) {
		this(ring, false);
	}
	
	protected Clock(Ring ring, boolean measureLatency_) {
		super("clock", ring);
		measureLatency = measureLatency_;
		nextScheduled = Long.MAX_VALUE;
		nextActionIndex = 0;
		lastNanoTime = 0;
		incoming = 0;
		latency = new Latency.I("CycleLatency", 1000);
		bookToOrderLat = new Latency.I("BookToOrder", 50);
		schedule();
	}
	
	public void schedule(DateTime dateTime, Action actor) {
		schedule(TimeUnit.MILLISECONDS.toNanos(dateTime.getMillis()), actor);
	}
	
	static Logger log = LoggerFactory.getLogger(Clock.class);	
	
	public void schedule(long gmtNanos, Action actor) {
		checkArgument(gmtNanos >= gmtNanos(), "You can not schedule action in the past! Now it's %s and you are scheduling %s which is %s nanos ago", gmtNanos(), gmtNanos, gmtNanos() - gmtNanos);		
		actions.add(new ScheduledAction(nextActionIndex++, gmtNanos, actor));
		nextScheduled = actions.peek().nanos;
	}

	public long cycle() {
		return cycle;
	}
	
	@Override
	public void update() {
		if(measureLatency) {
			long nanotime = System.nanoTime();
			if(lastNanoTime > 0) {
				latency.add(nanotime - lastNanoTime);
				if((nanotime - lastNanoTime) > 100_000_000 && latencyWarnings > 0) {
					// our first gc call after JIT compiling might take some while. so we don't scare ops people 
					if(!ignoreCycleWarn || (nanotime - lastNanoTime) > 300_000_000) {
						log.error("Cycle latency way too high: {} secs", (nanotime - lastNanoTime) / 1e9);
						latencyWarnings--;						
					}
					ignoreCycleWarn = false;
				}
			}
			incoming = 0;
			lastNanoTime = nanotime;
		}
		cycle++;
		// run scheduled stuff
		long now = gmtNanos();
		ScheduledAction pk = actions.peek();
		while(pk != null && !(pk.nanos > now)) {
			pk = actions.poll();
			assert pk != null;
			pk.nanos = pk.action.scheduledAction(now);			
			// re-schedule if desired
			if(pk.nanos > now) {				
				actions.add(pk);
			} else if(pk.nanos > 0) {
				throw new IllegalArgumentException("Can not re-schedule actions in the past " + pk + ". Now is " + fromNanos(now));
			}
			pk = actions.peek();
		}
		if(pk != null) {
			nextScheduled = actions.peek().nanos;
		}
		// reschedule on loop
		reschedule();
	}
	
	public Latency cycleLatency() {
		return latency;
	}
	
	public Latency bookToOrderLatency() {
		return bookToOrderLat;
	}
	
	public void incoming() {
		if(incoming == 0) {
			incoming = lastNanoTime;
		}
	}
	
	public void outgoing() {
		if(incoming > 0) {
			lastBookToOrderTime = System.nanoTime() - incoming;
			bookToOrderLat.add(lastBookToOrderTime);
			incoming = 0;
		}
	}
	
	public long lastBookToOrderTime() {
		return lastBookToOrderTime;
	}
	
	protected abstract void reschedule();

	
}
