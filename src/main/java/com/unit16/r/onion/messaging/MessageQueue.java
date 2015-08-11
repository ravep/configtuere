package com.unit16.r.onion.messaging;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.unit16.r.onion.Component;
import com.unit16.r.onion.Ring;
import com.unit16.r.onion.util.Yield;
import com.unit16.z.Pair;

/** Message Queue used to wire up Source<T> with targets over thread boundaries 
 *
 * Message Queue is used by MessageScheme::connect
 */
public interface MessageQueue {

	public void incoming(Dispatcher d, Object msg);

	public static class I extends Component implements MessageQueue {

		private final Yield yield;
		private final ConcurrentLinkedQueue<Pair<Dispatcher, Object>> queue = new ConcurrentLinkedQueue<>();
		
		public I(Ring ring_, Yield yield_) {
			super("MessageQueue", ring_);
			yield = yield_;	
			schedule();
		}

		@Override
		public void incoming(Dispatcher d, Object msg) {
			queue.add(Pair.I.pair(d, msg));
		}

		@Override
		protected void update() {
			Pair<Dispatcher, Object> nxt = queue.poll();
			if(nxt != null) {
				nxt.fst().dispatch(nxt.snd());
			}
			yield.yield(this);
		}
		
	}
	
}
