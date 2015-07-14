package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

public abstract class Group {
	
	public interface Checker {

		public abstract boolean validate(Config c);
		public String error();
		
		public abstract static class I implements Checker {
			private String error;
			protected void setError(String err) { error = err; }
			@Override public abstract boolean validate(Config c);
			@Override public String error() { return error; }
			
			protected boolean exclusive(Config c, C<?> a, C<?> b, boolean required) {
				if(c.isset(a) && c.isset(b)) {
					setError(String.format("%s and %s are exclusive", a.name, b.name));
					return false;
				}
				if(required && !c.isset(a) && !c.isset(b)) {
					setError(String.format("either %s or %s is required", a.name, b.name));
					return false;
				}	
				return true;
			}			
			
		}
		
	}
	
	public enum RelationFlag {
		OPTIONAL(false, false),
		REQUIRED(false, true),
		ATLEASTONE(true, true), // 1 .. N
		MULTIPLE(true, false) // 0 .. N
		;
		public final boolean repeatable;
		public final boolean required;
		RelationFlag(boolean repeatable_, boolean required_) {
			repeatable = repeatable_;
			required = required_;
		}
	}
	
	public interface Dependency extends GroupGetter {
		
		public RelationFlag flag();
		@Override public Group group();
		
		public static class I implements Dependency {
			final Group g;
			final RelationFlag f;
			I(Group g_, RelationFlag f_) {
				g = g_;
				f = f_;
			}
			@Override public RelationFlag flag() {
				return f;
			}
			@Override public Group group() {
				return g;
			}
		}
	}
	
	final String name;
	final List<Dependency> relations;
	final List<C<?>> values;
	final List<Checker> checks;
	
	protected Group(String name_, List<Checker> checks_) {
		assert name_.indexOf('.') == -1;
		name = name_;
		relations = new LinkedList<>();
		checks = checks_;
		values = new LinkedList<>();
	}
	
	public abstract void init();
	
	@Override
	public String toString() {
		return name;
	}
	
	public String name() {
		return name;
	}

	public Dependency relation(String name) {
		for(Dependency b : relations) {
			if(b.group().name.equals(name)) {
				return b;
			}
		}
		throw new IllegalArgumentException(String.format(
			"Group '%s' does not know the subgroup '%s'. There are %d valid subgroups (%s) and %d possible values (%s)",
			this.name, name, relations.size(), Joiner.on(",").join(
				Iterables.transform(
					Iterables.transform(relations, Group.GETGROUP),
					Group.GETNAME)
			),
			values.size(),
			Joiner.on(",").join(Iterables.transform(values, C.TONAME))
		));
	}

	public List<Dependency> relations() {
		return Collections.unmodifiableList(relations);
	}
	
	public C<?> value(String name) {
		if(values.size() == 0) {
			throw new IllegalArgumentException("Invalid request for value " + name + " in group " + this.name + ". Group doesn't have any values");
		}
		for(C<?> v : values) {
			if(v.name.equals(name)) {
				return v;
			}
		}
		throw new IllegalArgumentException("No such value definition: " + name + " for group " + name()
				+ (values.size() > 0 ? (". Valid variables are " + Joiner.on(",").join(Iterables.transform(values, C.TONAME)))
					: ". No variables in this group.")
				+ (relations.size() > 0 ? (". Valid subgroups are " + Joiner.on(",").join(Iterables.transform(relations, Functions.compose(Group.GETNAME, Group.GETGROUP))))
					: ". No subgroups in this group.")
		);
	}


	public List<C<?>> values() {
		return Collections.unmodifiableList(values);
	}
	
	public boolean equals(Group o) {
		return o.name == this.name;
	}
	
	public static Function<Group, String> GETNAME = new Function<Group,String>() {

		@Override
		public String apply(Group input) {
			return input.name;
		}
		
	};
	
	public interface GroupGetter {
		Group group();
	}
	
	public static Function<GroupGetter, Group> GETGROUP = new Function<GroupGetter,Group>() {

		@Override
		public Group apply(GroupGetter input) {
			return input.group();
		}
		
	};
	
	
	
	

	
}
