package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import com.unit16.conf.C.Checker;


public abstract class CFactory<T> {		
	
	final String name;
	final Group group;
	T defaultValue;
	boolean required;
	String[] restrictedValues;
	List<Checker<T>> checkers;
	Parser<T> parser;
	
	public CFactory(Group group_, String name_, Parser<T> parser_) {
		checkNotNull(parser_);
		checkNotNull(group_);
		checkNotNull(name_);
		group = group_;
		defaultValue = null;
		name = name_;
		checkers = new LinkedList<>();
		required = true;
		parser = parser_;
		restrictedValues = null;
	}
	
	public CFactory<T> Default(T defaultValue_) {
		defaultValue = defaultValue_;
		required = false;
		return this;
	}
	
	public CFactory<T> optional() {
		required = false;
		return this;
	}

	@SafeVarargs
	public final <E> CFactory<T> restrictedToValues(E... values) {
		restrictedValues = new String[values.length];
		for(int ii = 0; ii < values.length; ii++) {
			restrictedValues[ii] = values[ii].toString();
		}		
		return this;
	}
	
	public CFactory<T> check(Checker<T> t) {
		checkers.add(t);
		return this;
	}
		
	public CFactory<T> regexp(final String regexp) {
		// wrap regexp checker around parser
		parser = new Parser.RegExpParser<T>(parser, regexp);
		return this;
	}
	public abstract C<T> build();



		
}
