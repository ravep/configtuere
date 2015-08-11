package com.unit16.r.onion;

import java.util.ArrayDeque;

public interface Ring {

	void schedule(Component c);
	
	public static class I implements Ring {
	
		final Peeler runner;
		final int index;
		final String name;
		ArrayDeque<Component> queue;	
		
		I(Peeler runner_, int index_, String name_) {
			runner = runner_;
			index = index_;
			name = name_;
			queue = new ArrayDeque<>(8); 
		}
		
		boolean hasWork() {
			return !queue.isEmpty();
		}
		
		Component poll() {
			return queue.poll();
		}
		
		@Override
		public void schedule(Component c) {
			queue.add(c);
			runner.ringScheduled(index);
		}
	}
	
}
