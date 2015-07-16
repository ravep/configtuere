package com.unit16.r.onion;

import java.util.ArrayDeque;

import com.unit16.r.onion.util.Clock;
import com.unit16.r.onion.util.Clock.Action;

public class ThrottledRing implements Ring, Action {
	
	private final long throttleNanos;
	private final ArrayDeque<Component> queue = new ArrayDeque<>();
	private final Ring base;
	
	public ThrottledRing(Clock clock, long throttleNanos_, Ring base_) {
		throttleNanos = throttleNanos_;
		clock.schedule(clock.gmtNanos() + throttleNanos, this);
		base = base_;
	}
	
	@Override
	public void schedule(Component c) {
		queue.add(c);
	}

	@Override
	public long scheduledAction(long nanotime) {
		if(queue.size() > 0) {
			for(Component c : queue) {
				base.schedule(c);
			}
			queue.clear();
		}
		return nanotime + throttleNanos;
	}

}
