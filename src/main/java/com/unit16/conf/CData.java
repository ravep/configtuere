package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import com.unit16.conf.C.Checker;


public class CData<T> {
	
	public final C<T> type;
	String valueStr;
	T value;
	String parseError;
	String error;
	
	public CData(C<T> type_) {
		type = type_;
		value = null;
		valueStr = "";
		parseError = null;			
	}
	
	public CData(C<T> type_, T value_) {
		type = type_;
		value = value_;
		valueStr = value_ != null ? type.parser.toString(value_) : "";
		parseError = null;
	}
	
	public CData(C<T> type_, String valueStr_) {
		type = type_;
		set(valueStr_);
	}
	
	public void set(T value_) {
		value = value_;
		valueStr = type.parser.toString(value_);
		parseError = null;
	}
	
	public void set(String valueStr_) {
		valueStr = valueStr_;
		value = null;
		parseError = null;
		try {
			value = type.parser.fromString(valueStr);
		} catch(Exception e) {
			parseError = e.getMessage();
		}
	}

	public boolean validate() {
		error = null;
		if(value == null) {
			if(parseError != null) {
				error = parseError;
			} else {
				error = "Value is null";
			}
			return false;
		}
		
		for(Checker<T> c : type.checkers) {
			if(!c.validate(value, type)) {
				error = c.error();
				return false;			
			}
		}
		return true;
	}
	
	public boolean hasValue() {
		return value != null;
	}

	public String valueAsString() {			
		return valueStr;
	}
	
	public T value() {
		if(hasValue()) {
			return value;
		} else {
			return type.defaultValue;
		}
	}
		

	public String errors() {
		return error;
	}
			
}

