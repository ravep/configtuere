package com.unit16.r.onion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public abstract class Component  {
	
	public static String mkname(Object ... args) {
		return Joiner.on(",").join(args);
	}
	
	public static String rchr(String str) {
		return str.replace('.', '_').replace('/','_');
	}
	
	public static String mkname(Class<?> klass, Object ... args) {
		String[] sp = klass.getName().split("\\.");
		int idx = sp.length - 1;
		String cl = sp[idx];
		while(cl.length() < 5 && idx-- > 0) {
			cl = sp[idx] + "." + cl;
		}
		return cl + "[" + Joiner.on(",").join(args) + "]";
	}
	
	protected Logger log;
	
	private final String name;
	private final Ring ring;
	private boolean scheduled;
	
	public Component(String name_, Ring ring_) {
		ring = ring_;
		scheduled = false;
		name = name_;
		log = LoggerFactory.getLogger(this.getClass().getName() + "[" + name.replace('.', '_') + "]");
	}
	
	public String name() {
		return name;
	}
	
	protected abstract void update();
	
	public final void process() {
		assert scheduled;
		scheduled = false;
		update();
	}
	
	public final void schedule() {
		if(!scheduled) {
			scheduled = true;
			ring.schedule(this);
		}
	}

}
