package com.unit16.r.onion.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.unit16.r.onion.Component;
import com.unit16.r.onion.Ring;

public interface Yield {

	public void yield(Component c);
	
	public static class I extends Component  implements Yield {
		
		private final List<Component> queue = new ArrayList<>();
		
		public I(String name, Ring ring_) {
			super(name, ring_);
		}

		@Override
		protected void update() {
			for(Component c : queue) {
				c.schedule();
			}
			queue.clear();
		}

		@Override
		public void yield(Component c) {
			queue.add(c);
			schedule();		
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(Yield.class)
				.add("name", name())
				.add("queueSize", queue.size())
				.toString();
		}
		
	}
	
}
