package com.unit16.r.panic;

import static com.google.common.base.Preconditions.checkArgument;

import com.unit16.r.onion.util.Clock;
import com.unit16.r.onion.util.Clock.Action;

public class DelayedPanicDelegate extends PanicDelegate implements Action {

	private final Clock clock;
	private final long panicTime;
	private long panicEventTime;
	private boolean warning;
	private String panicMsg;
	
	public DelayedPanicDelegate(String name_, Clock clock_, long panicTimeNanos) {
		super(name_);
		checkArgument(panicTimeNanos > 0);
		clock = clock_;
		panicTime = panicTimeNanos;
		panicMsg = null;
		warning = false;
	}

	@Override public void broken(String msg) {
		if(panicMsg == null) {
			log.warn("Broken?: {}", msg);
			clock.schedule(clock.gmtNanos() + panicTime, this);
			panicMsg = msg;
			panicEventTime = clock.gmtNanos();
		} else if(!warning && panicEventTime + panicTime / 2 < clock.gmtNanos()) {
			warning = true;
			log.warn("Panic since {} seconds: {}", (clock.gmtNanos() - panicEventTime) / 1e9, panicMsg);
		}
	}
	
	@Override 
	public boolean isBroken() {
		return super.isBroken() || panicMsg != null;
	}
	
	@Override
	public long scheduledAction(long nanotime) {
		if(panicMsg != null) {
			super.broken(panicMsg);
		}
		return 0;
	}
	
	@Override 
	public void resolved() {
		if(temporary != null || panicMsg != null) {
			log.info("Resolved: {}", panicMsg);
			panicMsg = null;
			warning = false;
			panicEventTime = 0;
			if(temporary != null) {
				super.resolved();
			}
		}
	}
		
}
