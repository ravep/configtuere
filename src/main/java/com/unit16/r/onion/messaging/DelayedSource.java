package com.unit16.r.onion.messaging;

import java.util.LinkedList;
import java.util.Queue;

import com.unit16.r.onion.messaging.MessageScheme.PostBuildListener;
import com.unit16.r.onion.util.Clock;
import com.unit16.r.onion.util.Clock.Action;

public abstract class DelayedSource<T> extends Source<T> implements PostBuildListener, Action {

	private final Clock clock;
	private final MessageScheme scheme;
	private final Source<T> base;
	private boolean connected = false;
	private final Queue<T> queue = new LinkedList<>();
	protected final long nanoDelay;
	
	public DelayedSource(Source<T> b, Clock clock_, MessageScheme s, long nanoDelay_) {
		super(mkName(b.name,nanoDelay_), b.type);
		s.registerPostBuild(this);
		scheme = s ;
		base = b;
		clock = clock_;
		nanoDelay = nanoDelay_;
	}
	
	private static String mkName(String name, long delay) {
		return String.format("Delayed[%g,%s]", (double)delay, name);
	}

	@Override
	public boolean wireUp() {
		if(!connected && active()) {
			scheme.connect(base, this);
			connected = true;
			return true;
		}
		return false;
	}
	
	public abstract void handle(T obj);
	
	protected void process(T obj) {
		queue.add(obj);
		clock.schedule(delayedNanotime(clock.gmtNanos()), this);
	}
	
	protected long delayedNanotime(long nanotime) {
		return nanotime + nanoDelay;
	}
	
	@Override
	public long scheduledAction(long nanotime) {
		assert queue.size() > 0;
		tell(queue.poll());
		return 0;
	}
	

	
}
