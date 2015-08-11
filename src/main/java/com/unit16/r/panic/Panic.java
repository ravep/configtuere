package com.unit16.r.panic;

import com.google.common.base.MoreObjects;

/** panic class
 * 
 * Panics can be persistent which means they are just fired once. Or they
 * can be persistent which means that they are active until they are 
 * resolved.
 * 
 * An example of a PERSISTENT panic should be if we receive an unexpected
 * reject or we have a parsing problem of a base feed. In that case it's no
 * longer safe to continue running the strategy and we need to start 
 * shutting the affected pieces down.
 * 
 * A TEMPORARY panic message means that there is a temporary problem but it 
 * might be resolvable such as a remote feed drop out or a client heartbeat 
 * issue. 
 */
public class Panic {
 	
	public static synchronized Panic create(String msg, Type type, Object src) {
		return new Panic(nextId++, src, msg, type);
	}
	
	public enum Type {
		FATAL,
		TEMPORARY,
		RESOLVED_TEMPORARY
	}
		
	private static int nextId = 1;
	
	public final String msg;
	public final int id;
	public final Type type;
	
	final Object src;
	private final String str;
	
	Panic(int id_, Object src_, String msg_, Type tp) {
		msg = msg_;
		src = src_;
		id = id_;
		type = tp;
		str = MoreObjects.toStringHelper(getClass())
			.add("id", id)
			.add("type", type)
			.add("msg", msg)
			.toString();
	}
	
	public String toString() {
		return str;
	}
	
	public Panic resolved() {
		assert type == Type.TEMPORARY;
		return new Panic(id, src, msg, Type.RESOLVED_TEMPORARY);
	}
	
	@Override public int hashCode() {
		return id;
	}
	
	@Override public boolean equals(Object o) {
		if(o instanceof Panic) {
			Panic c = (Panic)o;
			return c.id == id;
		}
		return false;
	}
	
}
