package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;


public class C<T> {
	
	public interface Checker<T> {	
		boolean validate(T value, C<T> def);
		String docString();
		String error();
		
		public abstract static class I<T> implements Checker<T> {
			final String docString;
			String error;
			protected I(String docString_) {
				docString = docString_;				
			}
			protected void setError(String err) {
				error = err;
			}
			@Override public String error() {
				return error;
			}
			@Override public String docString() {
				return docString;
			}
		}
		
	}

	public final String name;
	public final T defaultValue;	
	public final boolean required;
	public final List<Checker<T>> checkers;
	public final Parser<T> parser;
	protected final List<String> restrictedValues;
		
	protected C(String name_, T defaultValue_, boolean required_, List<String> restrictedValues_, List<Checker<T>> c, Parser<T> p) {
		assert name_.indexOf('.') == -1;
		name = name_;
		defaultValue = defaultValue_;
		required = required_;
		checkers = c;
		parser = p;
		restrictedValues = restrictedValues_;
	}
	
	public String defaultValueAsString() {
		return parser.toString(defaultValue);
	}
	
	public boolean hasDefaultValue() {
		return defaultValue != null;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass())
			.add(name, name)
			.add("defaultValue", defaultValue)
			.add("required", required)
			.add("checkers", Joiner.on(",").join(checkers))
			.add("restricted", Joiner.on(",").join(restrictedValues))
			.toString();
	}
	
	public static  Function<C<?>,String> TONAME = new Function<C<?>,String>() {
		@Override
		public String apply(C<?> input) {
			return input.name;
		}
	};
	
	public boolean hasRestrictedValues() {
		return restrictedValues.size() > 0; 
	}
	
	public List<String> restrictedValues() {
		return restrictedValues;
	}
	
	
	
	
}
