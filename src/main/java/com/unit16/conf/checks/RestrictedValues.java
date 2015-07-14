package com.unit16.conf.checks;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.util.List;

import com.unit16.conf.C;
import com.unit16.conf.C.Checker;
import com.unit16.conf.Parser;

public class RestrictedValues<T> implements Checker<T> {

	final List<String> values;
	final Parser<T> parser;
	String error;
		
	public RestrictedValues(Parser<T> parser_, List<String> values_) {
		parser = parser_;
		values = values_;
		error = null;
	}
	
	@Override
	public boolean validate(T value, C<T> def) {
		String cmp = parser.toString(value);
		for(String s : values) {
			if(s.equals(cmp)) {
				return true;
			}
		}
		error = String.format("'%s' is not valid. %s", value, docString());
		return false;
	}

	@Override
	public String docString() {
		StringBuilder b = new StringBuilder();
		b.append("Valid options are: ");
		String delim = "";
		for(String s : values) {
			b.append(delim);
			b.append(s);
			delim = ", ";
		}
		return b.toString();
	}

	@Override
	public String error() {		
		return error;
	}

}
