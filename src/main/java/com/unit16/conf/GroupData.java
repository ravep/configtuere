package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.util.List;

import com.unit16.conf.Group.GroupGetter;

public interface GroupData extends GroupGetter {

	List<CData<?>> values();
	void set(CData<?> value);
	List<String> errors();
	boolean validate();
	
}
