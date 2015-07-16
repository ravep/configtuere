package com.unit16.r.onion.messaging;

public abstract class DispatcherImpl extends Dispatcher {

	protected final Object dest;
	
	protected DispatcherImpl(Object dest_, Dispatcher next_) {
		super(next_);
		dest = dest_;
	}
	
	@Override
	public abstract void dispatch(Object msg);
	
	@Override
	public boolean active() {
		return true;
	}
}
