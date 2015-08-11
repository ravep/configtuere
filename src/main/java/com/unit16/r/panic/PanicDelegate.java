package com.unit16.r.panic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.unit16.r.onion.messaging.Source;
import com.unit16.r.panic.Panic.Type;

public class PanicDelegate {

	protected static Logger log = LoggerFactory.getLogger(PanicDelegate.class);
	
	private final String name;
	private final Source<Panic> src;
	protected Panic temporary;
	private boolean guard = false;
	
	public PanicDelegate(String name_) {
		name = name_;
		src = new Source<Panic>(Panic.class, name, Panic.class);
		temporary = null;
	}
	
	public Source<Panic> source() {
		return src;
	}
	
	public void fatal(String fmt, Object ... arg) {
		fatal(String.format(fmt, arg));
	}
	
	public void fatal(String msg) {
		log.error("Emitting PANIC {} {}", name, msg);
		try {
			if(!src.active()) {
				log.error("My panic source is not connected to any sink!");
			}
			src.tell(Panic.create(msg, Type.FATAL, this));
		} catch(Exception e) {
			log.error("Received exception while processing PANIC message! Game over ...");
			throw new RuntimeException(e);
		}
	}
	
	public void broken(String msg) {
		if(temporary == null) {
			log.error("Issue detected: {}", msg);
			temporary = Panic.create(msg, Type.TEMPORARY, this);
			src.tell(temporary);
		}
	}
		
	public boolean isBroken() {
		return temporary != null;
	}
	
	public void resolved() {
		if(temporary != null) {
			log.warn("Good again: {}", temporary.msg);
			src.tell(temporary.resolved());
			temporary = null;
		}
	}

	public void forward(Panic p) {
		if(guard) {
			return;
		}
		guard = true;
		try {
			if(p.src == this) {
				log.error("Recursive panic call reached {}. Not forwarding it any more! Stack trace is:\n{}",
					name, Joiner.on("\n").join(Thread.currentThread().getStackTrace()));
			} else {
				src.tell(p);
			}			
		} finally {
			guard = false;
		}
	}

	
}
