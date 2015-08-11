package com.unit16.r.panic;

import java.util.HashSet;

public class PanicTracker {

	private final HashSet<Panic> panics = new HashSet<>();
	
	public PanicTracker() {
		
	}
	
	public void handle(Panic p) {
		switch(p.type) {
		case FATAL:
			break;
		case RESOLVED_TEMPORARY:
			assert panics.contains(p);
			panics.remove(p);
			break;
		case TEMPORARY:
			panics.add(p);
			break;
		default:
			throw new RuntimeException("Unknown panic type! " + p.type);
		}
	}
	
	public boolean good() {
		return panics.size() == 0;
	}
	
}
