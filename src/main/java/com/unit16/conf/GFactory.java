package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.util.LinkedList;

import com.unit16.conf.Group.Checker;
import com.unit16.conf.GroupDef.DelayedInit;

public class GFactory {
	
	final String name;
	final LinkedList<Checker> checks = new LinkedList<>();
	DelayedInit delayedInit;
	
	public GFactory(String name_) {
		name = name_;		
	}
		
	public GFactory init(DelayedInit d) {
		delayedInit = d;
		return this;
	}
	
	public GFactory check(Checker c) {
		checks.add(c);
		return this;
	}

	public GroupDef build() {
		return new GroupDef(name, delayedInit, checks);
	}

	
}
