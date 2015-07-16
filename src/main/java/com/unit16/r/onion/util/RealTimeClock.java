package com.unit16.r.onion.util;

import com.unit16.r.onion.Ring;

public class RealTimeClock extends Clock {

	private long currentTime;
	
	private final Yield yield;
	private boolean started = false;
	
	public RealTimeClock(Ring ring, Yield yield_) {
		super(ring, true);
		yield = yield_;
	}
	
	protected long current() {
		return System.currentTimeMillis() * 1_000_000l;
	}

	@Override
	public long gmtNanos() {
		if(started) {
			return currentTime;
		} else {
			return current();
		}
	}
	
	@Override
	public void update() {
		started = true;
		currentTime = current();
		super.update();
	}

	@Override
	protected void reschedule() {
		yield.yield(this);
	}
}
