package com.unit16.r.onion.messaging;

/** Message dispatcher class
 *
 *  A dispatcher has a simple task: It dispatches messages of type T to a destination
 *  object of type D. D must implement a method named public void handle(T msg);
 *  
 *  Then, a call to dispatch(msgT) will invoke ((D)dest).handle((T)msg)
 *  and subsequently invoke next.dispatch(msgT)
 *  
 *  The dispatcher objects are created and removed by a MessageScheme.
 *  The dispatcher objects are joined together by pointers (linked list).
 */
public abstract class Dispatcher {

	protected final Dispatcher next;	
	public Dispatcher(Dispatcher next_) {
		next = next_;
	}
	public abstract void dispatch(Object msg);
	public abstract boolean active();
}
