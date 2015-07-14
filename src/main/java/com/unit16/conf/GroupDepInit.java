package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */

import com.unit16.conf.Group.RelationFlag;

public interface GroupDepInit {

	void atleastone(Group ... group);
	void atleastone(Iterable<Group> groups);
	void required(Group ... group);
	void required(Iterable<Group> groups);
	void multiple(Group ... group);
	void multiple(Iterable<Group> groups);
	void optional(Group ... group);
	void add(RelationFlag flag, Group... group);
	void add(RelationFlag flag, Iterable<Group> group);
	void oneOf(Group... groups);
	void oneOf(Iterable<Group> groups);
	void mergeValues(C<?>... values);
	void mergeGroup(GroupDef conf);
	void mergeValues(GroupDef conf);
	
}
