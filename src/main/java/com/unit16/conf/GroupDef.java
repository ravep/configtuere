package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.unit16.conf.checks.RestrictedValues;


public class GroupDef extends Group {
	
	public interface DelayedInit {
		void init(GroupDepInit d);
	}
	
	private boolean initialized;
	private final DelayedInit delayedInit;
		
	protected GroupDef(String name_, DelayedInit delayedInit_,
			List<Checker> checks_) {
		super(name_, checks_);
		initialized = false;		
		delayedInit = delayedInit_;
	}
	
	public static GFactory create(String name) {
		return new GFactory(name);
	}
	
	public GFactory createClone(String name) {
		final GroupDef base = this;
		return create(name).init(new DelayedInit() {
			@Override
			public void init(GroupDepInit d) {
				d.mergeGroup(base);
			}
		});
	}
	
	<T> C<T> build(CFactory<T> r) {		
		List<String> restrictedValues = new ArrayList<>();
		// add checker for restricted values
		List<C.Checker<T>> checkers = new ArrayList<>(r.checkers);
		if(r.restrictedValues != null) {
			restrictedValues.addAll(Arrays.asList(r.restrictedValues));
			checkers.add(new RestrictedValues<T>(r.parser, restrictedValues));
		}
		C<T> tmp = new C<T>(r.name, r.defaultValue, 
				r.required,
				restrictedValues,
				checkers, r.parser);
		addValue(tmp);
		return tmp;
	}
	
	public <T> CFactory<T> variable(String name, Parser<T> p) {
		return new CFactory<T>(this, name, p) {
			@Override public C<T> build() {
				return GroupDef.this.build(this);
			}
		};
	}
	
	private <T extends Comparable<T>> ComparableFactory<T> compVariable(String name, Parser<T> p) {
		return new ComparableFactory<T>(this, name, p) {
			@Override public C<T> build() {
				return GroupDef.this.build(this);
			}	
		};
	}
	
	public ComparableFactory<Double> dbl(String name) {
		checkArgument(!initialized);
		return compVariable(name, Parser.I.DBL);
	}
	public ComparableFactory<Integer> integer(String name) {
		checkArgument(!initialized);
		return compVariable(name, Parser.I.INT);
	}
	public ComparableFactory<Long> longV(String name) {
		checkArgument(!initialized);
		return compVariable(name, Parser.I.LONG);
	}

	public CFactory<Boolean> bool(String name) {
		checkArgument(!initialized);
		CFactory<Boolean> cf = variable(name, Parser.I.BOOL);
		cf.restrictedValues = new String[]{"yes","no"};
		return cf;
	}
	public CFactory<String> string(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.STRING);
	}
	public CFactory<List<Double>> ldbl(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.LDBL);
	}
	public CFactory<List<Integer>> lint(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.LINT);
	}
	public CFactory<List<String>> lstr(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.LSTR);
	}
	public <T extends Enum<T>> CFactory<T> enm(String name, T[] values) {
		checkArgument(!initialized);
		return variable(name, Parser.I.ENUM(values)).restrictedToValues(values);
	}
	public CFactory<LocalDate> localDate(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.LOCALDATE);
	}
	public CFactory<LocalTime> localTime(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.LOCALTIME);
	}
	public CFactory<DateTimeZone> timeZone(String name) {
		checkArgument(!initialized);
		return variable(name, Parser.I.TIMEZONE);
	}
	public CFactory<File> file(String name, boolean mustExist, boolean isDirectory) {
		checkArgument(!initialized);
		CFactory<File> f = variable(name, Parser.I.FILE); 
		if(mustExist) {
			f.check(new C.Checker.I<File>("File must exists") {
				@Override
				public boolean validate(File value, C<File> def) {
					if(!value.exists()) {
						setError(value.getAbsolutePath() + " does not exist");
						return false;
					}
					return true;
				}
			});
		}
		if(isDirectory) {
			f.check(new C.Checker.I<File>("File must be a directory") {
				@Override
				public boolean validate(File value, C<File> def) {
					if(value.exists() && !value.isDirectory()) {
						setError(value.getAbsolutePath() + " is not a directory");
						return false;
					}
					return true;
				}
			});
		}
		return f;
	}	
	
	public CFactory<Pattern> pattern(String name) {
		return variable(name, Parser.I.PATTERN);
	}

	@Override
	public void init() {
		if(!initialized) {
			initialized = true;
			if(delayedInit != null) {
				delayedInit.init(new GroupDepInit() {
					@Override public void add(final RelationFlag flag, final Group... groups) {
						add(flag, Arrays.asList(groups));
					}
					@Override public void add(final RelationFlag flag, final Iterable<Group> groups) {
						for(Group g : groups) {
							addDependency(new Dependency.I(g, flag));							
						}
					}

					@Override
					public void oneOf(final Group... groups) {
						oneOf(Arrays.asList(groups));
					}

					@Override
					public void oneOf(final Iterable<Group> groups) {
						add(RelationFlag.OPTIONAL, groups);
						checks.add(new Checker.I() {
							@Override
							public boolean validate(Config c) {
								Group found = null;
								for(Group g : groups) {
									if(c.numSub(g) > 0) {
										if(found != null) {
											setError("Groups " + found.name + " and " + g.name + " are exclusive");											
											return false;										
										}
										found = g;										
									}
								}
								if(found == null) {
									setError("Need one group of " + Joiner.on(",").join(
										Iterables.transform(groups, Group.GETNAME))
									);
									return false;
								}
								return true;
							}
						});
					}
					@Override
					public void atleastone(Iterable<Group> group) {
						add(RelationFlag.ATLEASTONE, group);
					}

					@Override
					public void atleastone(Group... group) {
						add(RelationFlag.ATLEASTONE, group);						
					}
					@Override
					public void required(Group... group) {
						required(Arrays.asList(group));
					}
					@Override
					public void required(Iterable<Group> group) {
						add(RelationFlag.REQUIRED, group);												
					}
					@Override
					public void multiple(Group... group) {
						multiple(Arrays.asList(group));
					}
					@Override
					public void multiple(Iterable<Group> group) {
						add(RelationFlag.MULTIPLE, group);												
					}
					@Override
					public void optional(Group... group) {
						add(RelationFlag.OPTIONAL, group);												
					}					
					@Override
					public void mergeValues(C<?>... values_) {
						for(C<?> v : values_) {
							addValue(v);
						}
					}
					@Override
					public void mergeValues(GroupDef conf) {
						for(C<?> v : conf.values()) {
							addValue(v);
						}						
					}

					@Override
					public void mergeGroup(GroupDef conf) {
						for(C<?> v : conf.values()) {
							addValue(v);
						}
						// hope that nobody tries to do a circular reference ...
						if(!conf.initialized) {
							conf.init();
						}
						for(Dependency dep : conf.relations) {
							addDependency(dep);
						}
						// merge checks
						for(Checker c : conf.checks) {
							addCheck(c);
						}
					}

				});
			}
			for(Dependency c : relations) {
				c.group().init();
			}
		}
	}

	private void addDependency(Dependency d) {
		for(Dependency o : relations) {
			if(o.group() == d.group()) {
				throw new IllegalArgumentException(String.format("Subgroup %s added twice as dependency to %s",
					d.group(), this.name));
			}
		}
		relations.add(d);
	}

	private void addCheck(Checker c) {
		checks.add(c);
	}

	private void addValue(C<?> v) {		
		for(C<?> o : values) {
			if(o.name.equals(v.name)) {				
				throw new IllegalArgumentException("Duplicate value with name '" + v + "' in group " + name);
			}
		}
		values.add(v);
	}

	
}
