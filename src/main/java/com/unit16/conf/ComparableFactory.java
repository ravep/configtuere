package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import com.google.common.collect.Range;
import com.unit16.conf.C.Checker;

public abstract class ComparableFactory<T extends Comparable<T>> extends CFactory<T> {

	public ComparableFactory(Group group_, String name_, Parser<T> parser_) {
		super(group_, name_, parser_);
	}
	
	public ComparableFactory<T> check(final Range<T> r) {
		super.check(new Checker.I<T>(r.toString()) {

			@Override
			public boolean validate(T value, C<T> def) {
				if(r.contains(value)) {
					return true;
				} else {
					error = "Value " + value + " is not in range " + r;
					return false;
				}
			}
			
		});
		return this;
	}

}
