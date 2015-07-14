package com.unit16.conf.checks;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import com.unit16.conf.C;
import com.unit16.conf.C.Checker;

public class Strlen extends Checker.I<String> {

	private final int length;
	
	public Strlen(int length_) {
		super("String length of " + length_);
		length = length_;		
	}
	
	@Override
	public boolean validate(String value, C<String> def) {
		if(value != null && value.length() != length) {
			 setError(String.format("Expected %d characters, found %d", length, value.length()));
			 return false;
		}
		return true;
	}


}
