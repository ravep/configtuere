package com.unit16.r.onion.util;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.unit16.common.logback.MicrosConverter;
import com.unit16.r.onion.Component;
import com.unit16.r.onion.Ring;

public class SimulationClock extends Clock implements Yield {
    
	long nanos;
	final long stopNanos;
	private ArrayList<Component> coldYield = new ArrayList<>();
	
	public SimulationClock(Ring ring, DateTime start, DateTime stop) {
		super(ring, false);
		nanos = TimeUnit.MILLISECONDS.toNanos(start.getMillis());
		stopNanos = TimeUnit.MILLISECONDS.toNanos(stop.getMillis());
	}

	public boolean keepRunning() {
		return nanos < stopNanos;
	}
	
	@Override
	public void update() {
		assert nextScheduled > nanos;
		flushColdYield();
		nanos = nextScheduled;	
		MicrosConverter.updateTime(nanos);
		super.update();		
	}
	
	private void flushColdYield() {
		for(Component c : coldYield) {
			c.schedule();
		}
		coldYield.clear();
	}

	@Override protected void reschedule() {
		if(nanos < nextScheduled) {
			super.schedule();
		}
	}

	@Override
	public long gmtNanos() {
		return nanos;
	}

	public void clearQueue() {
		actions.clear();	
	}

	@Override
	public void yield(Component c) {
		coldYield.add(c);
	}


}
