package com.unit16.r.onion.messaging;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MessageScheme {
	
	public interface PostBuildListener {
		public boolean wireUp();
	}
	
	private final Logger log;
	private final List<PostBuildListener> postBuild;
	private final HashMap<Source<?>, List<Object>> connections;
	
	private static final Dispatcher EMPTY = new Dispatcher(null) {
		@Override
		public void dispatch(Object msg) {}
		@Override
		public boolean active() {
			return false;
		}		
	};
	
	
	public MessageScheme(String name) {
		 log = LoggerFactory.getLogger(MessageScheme.class.getName() + "[" + name + "]");
		 postBuild = new LinkedList<>();
		 connections = new HashMap<>();
	}
	
	public void prepare() {		
		if(postBuild.size() > 0 ) {
			log.debug("Starting post wiring of scheme");
			boolean done = false;
			while(!done) {			
				done = true;
				for(PostBuildListener l : postBuild) {
					if(l.wireUp()) {
						done = false;
					}
				}
			}
			log.debug("Done with post wiring");
		} else {
			log.debug("No delayed wirings for this scheme");
		}
	}
	
	public void registerPostBuild(PostBuildListener s) {
		postBuild.add(s);
	}
	
	private List<Object> connections(Source<?> s) {
		List<Object> o = connections.get(s);
		if(o == null) {
			connections.put(s, o = new ArrayList<>());
		}
		return o;
	}
	
	public <T> boolean connect(Source<T> s, final Object dest, final MessageQueue queueOfDest) {
		
		if(!isConnected(s, dest)) {
			return false;
		}
		
		final Dispatcher next = s.dispatcher.active() ? s.dispatcher : EMPTY;
		final Dispatcher unsafe = buildDispatcher(s, dest, EMPTY);				
		final Dispatcher safe = new Dispatcher(next) {
			@Override
			public void dispatch(Object msg) {
				queueOfDest.incoming(unsafe, msg);				
			}
			@Override
			public boolean active() {
				return true;
			}
		};
		
		s.dispatcher = safe;
		log.debug("Connected thread safe {} with {}", s, dest);		
		return true;
		
		
	}
	
	private <T> boolean isConnected(Source<T> s, final Object dest) {
		List<Object> recp = connections(checkNotNull(s));
		if(recp.contains(checkNotNull(dest))) {
			return false;
		}
		recp.add(dest);
		return true;
	}
	
	/** connect source with destination. return false if the connection already exists */
	public <T> boolean connect(Source<T> s, final Object dest) {
		if(!isConnected(s, dest)) {
			return false;
		}
		final Dispatcher next = s.dispatcher.active() ? s.dispatcher : EMPTY;
		Dispatcher disp = buildDispatcher(s, dest, next);						
		s.dispatcher = disp;
		log.debug("Connected {} with {}", s, dest);		
		return true;
	}

	protected <T> Dispatcher buildDispatcher(final Source<T> source, final Object dest, Dispatcher next) {
		try {
			final Method m = dest.getClass().getMethod("handle", checkNotNull(source.type));
			return new Dispatcher(next) {
				@Override
				public boolean active() {
					return true;
				}
				@SuppressWarnings("unchecked")
				@Override
				public void dispatch(Object o) {
					try {
						m.invoke(dest, (T)o);					
					} catch(Exception e) {
						throw new RuntimeException("Failed to dispatch " + o + " to " + dest + ": " + e.getMessage(), e);
					}
					next.dispatch(o);
				}
			};
		} catch (NoSuchMethodException e) {			
			throw new IllegalArgumentException("Failed to connect " + source + " and " + dest 
					+ ". Destination is missing the appropriate handle method: " 
					+ e.getMessage(), e);
		} 
	}

	public String dump() {
		StringBuilder b = new StringBuilder();
		b.append("Message scheme:");
		for(Source<?> s : connections.keySet()) {
			String str = s.toString();
			b.append("\n");
			b.append(str);
			for(Object o : connections.get(s)) {
				b.append("\n    -----> ");
				b.append(o);				
			}
		}
		return b.toString();
	}

	

}
