package com.unit16.r.onion.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class Source<T> {

	final static Logger log = LoggerFactory.getLogger(Source.class);
	final Class<T> type;
	final String name;
	final int hashCode;
	Dispatcher dispatcher; 	
	
	public Source(Class<?> definer, String name, Class<T> type_) {
		this(definer.getName() + "[" + name.replace('.', '_') + "]", type_);		
	}	
	
	public Source(Class<?> definer, Class<T> type_) {
		this(definer.getName(), type_);
	}
	
	protected Source(String name_, Class<T> type_) {
		name = name_;
		type = type_;
		dispatcher = new Dispatcher(null) {
			boolean warnUnused = false;
			@Override public boolean active() {
				return false;
			}
			@Override public void dispatch(Object o) {
				if(warnUnused) {
					warnUnused = false;
					log.warn("Wasting cpu cycles on dispatching unused message {} {}", name, type);
				}
			}
		};
		hashCode = (name + type.getName()).hashCode();
	}

	@Override public int hashCode() {
		return hashCode;
	}
	
	public boolean active() {
		return dispatcher.active();
	}

	public void tell(T o) {
		dispatcher.dispatch(o);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(Source.class)
			.add("name", name)
			.add("type", type.getName())
			.toString();
	}
	
}
