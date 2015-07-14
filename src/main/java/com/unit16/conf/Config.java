package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unit16.conf.Group.Dependency;
import com.unit16.conf.Group.RelationFlag;
import com.unit16.conf.io.BlockText;
import com.unit16.conf.io.SimpleText;



public class Config {

	public interface DelayedLoader {
		public void load(LocalDate date) throws Exception;
	}

	public static Config fromCommandLine(Group root, String[] args) throws Exception {
		final Config c = new Config(root);
		final Logger log = LoggerFactory.getLogger(Config.class);
		for(String s : args) {
			if(s.equals("--testConfig")) {
				continue;
			}
			if(s.startsWith("--")) {
				int idx = s.indexOf("=");				
				if(idx == -1) {
					SimpleText.subConfig(c, s.substring(2).split("\\."));
					continue;
				}
				final String key = s.substring(2, idx);
				final String val = s.substring(idx+1);
				final String inc = "include";
				if(key.equals(inc)) {
					BlockText.read(c, val);
				} else if(key.endsWith(inc)) {										
					BlockText.read(SimpleText.subConfig(c, key.substring(0, key.length() - inc.length()).split("\\.")), val);
				} else {
					try {
						SimpleText.setValue(c, key, val);
					} catch(Exception e) {
						throw new Exception("Invalid config option: " + s, e);
					}
				}
			} else {
				log.info("Loading config from '{}'", s);	
				SimpleText.read(c, s);
			}
		}
		// validate
		StringBuilder b = new StringBuilder();
		if(c.validate(b)) {
			log.info("Configuration looks good.");
		} else {
			log.error("Configuration errors:\n{}", b.toString());			
			throw new Exception("There are config errors ....");
		}
		return c;
	}
	
	private class MyGroupData implements GroupData {
		
		final Group group;
		final List<CData<?>> data;
		final List<MyGroupData> subs;		
		final List<String> errors;
		DelayedLoader loader;
		boolean loaded = false;
		
		MyGroupData(Group group_) {
			group = group_;
			data = new LinkedList<>();
			subs = new LinkedList<>();
			errors = new LinkedList<>();
			loader = null;
		}
		
		<T> boolean isset(C<T> t) {
			return findDataFor(t) != null;
		}
		
		<T> T get(C<T> t) {
			checkValidType(t);
			CData<T> dt = findDataFor(t);
			if(dt != null) {
				return dt.value;
			}
			else if(t.defaultValue != null) {
				return t.defaultValue;
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		<T> CData<T> findDataFor(C<T> type) {
			for(CData<?> dt : data) {
				if(dt.type.equals(type)) {
					return (CData<T>)dt;
				}
			}
			return null;
		}
		
		<T> void removeOld(C<T> type) {
			checkValidType(type);
			// remove old
			Iterator<CData<?>> dit = data.iterator();
			while(dit.hasNext()) {
				if(dit.next().type == type) {
					dit.remove();
				}
			}			
		}
		
		@Override
		public void set(CData<?> value) {
			removeOld(value.type);
			data.add(value);
		}
		
		<T> CData<T> set(C<T> type, String value) throws Exception {
			removeOld(type);
			CData<T> r;
			data.add(r = new CData<T>(type));
			r.set(value);
			return r;
		}
		
		<T> CData<T> set(C<T> type, T value) {
			removeOld(type);
			CData<T> r;
			data.add(r = new CData<T>(type, value));
			return r;
		}
		
		<T> void checkValidType(C<T> t) {
			checkArgument(group.values().contains(t),
				"config option '%s' does not belong to group '%s'",
				t.name, group.name);			
		}
		

		public void checkValidGroup(Group sub) {
			for(Dependency other : group.relations) {				
				if(other.group() == sub) {
					return;
				}
			}
			throw new IllegalArgumentException(String.format("Group '%s' is not a valid subgroup of '%s'",
				sub.name, group.name));			
		}
		
		int numSub(final Group sub) {
			int ii = 0;
			Iterator<MyGroupData> sit = subIterator(sub);
			while(sit.hasNext()) {
				sit.next();
				ii++;
			}
			return ii++;
		}
		
		Iterator<MyGroupData> subIterator(final Group sub) {
			
			return new Iterator<MyGroupData>() {
				MyGroupData myNext;
				Iterator<MyGroupData> sit;
				{
					sit = subs.iterator();
					myNext = getNext();
				}
				
				@Override
				public boolean hasNext() {
					return myNext != null;
				}

				@Override
				public MyGroupData next() {
					assert myNext != null;
					MyGroupData tmp = myNext;
					myNext = getNext();
					return tmp;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();						
				}
				
				MyGroupData getNext() {
					while(sit.hasNext()) {
						MyGroupData tmp = sit.next();
						if(tmp.group.equals(sub)) {
							return tmp;
						} 
					}
					return null;
				}
			};			
		}
		
		@Override public boolean validate() {
			// bypass if there is a delayed loader
			if(loader != null && !loaded) {
				return true;
			}
			errors.clear();
			// check all required values are set
			for(C<?> d : group.values) {				
				if(d.required && get(d) == null) {
					log.trace("Failed not null '{}' check", d.name);
					errors.add(String.format("'%s' is required but not set", d.name));
				} else {
					log.trace("Passed not null '{}' check {}", d.name, get(d));
				}				
			}
			// check values
			boolean invalidFields = false;
			for(CData<?> v : data) {				
				if(!v.validate()) {
					log.trace("Failed validate '{}' check: {}", v.type.name, v.error);
					errors.add(String.format("'%s' failed: %s", v.type.name, v.error));
					invalidFields = true;
				} else {
					log.trace("Passed validate '{}' check", v.type.name);
				}				
			}
			if(invalidFields) {
				errors.add("Group has values with errors");
			}
			// check that all required groups are set
			for(Dependency gr : group.relations) {
				int num = numSub(gr.group());
				if(num == 0 && (gr.flag() == RelationFlag.REQUIRED || gr.flag() == RelationFlag.ATLEASTONE)) {
					errors.add(String.format("subgroup '%s' is required", gr.group().name));
					log.trace("Failed subgroup '{}' required check", gr.group().name);
				}
				else if(num > 1 && (gr.flag() == RelationFlag.REQUIRED || gr.flag() == RelationFlag.OPTIONAL)) {
					errors.add(String.format("subgroup '%s' is only allowed once, found %d", gr.group().name, num));
					log.trace("Failed subgroup '{}' unique check", gr.group().name);
				} else {
					log.trace("Passed subgroup '{}' multipleness check", gr.group().name);
				}
			}
			// let the group checkers run over it
			if(errors.size() == 0 && group.checks.size() > 0) {
				Config c = new Config(this);
				for(Group.Checker chk : group.checks) {
					if(!chk.validate(c)) {
						errors.add(chk.error());
						break;
					}
				}
			}
			if(errors.size() > 0) {
				log.trace("Group '{}' check failed", group.name);				
			} else {
				log.trace("Group '{}' check passed", group.name);
			}
			return errors.size() == 0;
		}
				
		void walk(String prepend, String postpend, Walker walk) {
			if(prepend.length() > 0) {
				prepend += ".";
			}
			walk.walk(prepend, postpend, this);
			HashMap<String, Integer> count = new HashMap<String, Integer>();
			for(MyGroupData sub : subs) {
				if(group.relation(sub.group.name).flag().repeatable) {
					Integer idx = count.get(sub.group.name);
					if(idx == null) {
						idx = 0;
					}
					count.put(sub.group.name, idx + 1);
					sub.walk(prepend + group.name + postpend, "." + idx, walk);
				} else {
					sub.walk(prepend + group.name + postpend, "", walk);
				}
			}
		}

		@Override
		public Group group() {
			return group;
		}

		@Override
		public List<CData<?>> values() {
			return Collections.unmodifiableList(data);
		}

		@Override
		public List<String> errors() {
			return errors;
		}

		private void setDelayedLoader(DelayedLoader loader_) {
			checkArgument(loader == null);
			loader = loader_;
		}
		
		private boolean load(LocalDate date) throws Exception {
			if(loader != null) {
				loader.load(date);
				loaded = true;
			}
			for(MyGroupData dt : subs) {
				if(dt.load(date)) {
					loaded = true;
				}
			}
			return loaded;
		}

	}
	
	public interface Walker {
		public void walk(String prepath, String postpend, MyGroupData data);
	}
	
	final MyGroupData root;
	final Logger log;
	
	public Config(Group root_) {
		checkNotNull(root_);
		root_.init();
		log = LoggerFactory.getLogger(Config.class.getName() + "[" + root_.name.replace('.', '_') + "]");
		root = new MyGroupData(root_);		
	}
	
	private Config(MyGroupData root_) {
		checkNotNull(root_);
		log = LoggerFactory.getLogger(Config.class.getName() + "[" + root_.group.name.replace('.', '_') + "]");		
		root = root_;
	}
	
	public GroupData root() {
		return root;
	}

	public Collection<CData<?>> data() {
		return root.data;
	}
		
	public <T> CData<T> getData(C<T> type) {
		CData<T> r = root.findDataFor(type);
		if(r == null && type.hasDefaultValue()) {
			root.set(type, type.defaultValue);
			return getData(type);
		}
		return r;
	}

	public <T> boolean isset(C<T> t) {
		return root.isset(t);
	}

	public <T> T get(C<T> t) {
		return root.get(t);
	}	

	public <T> CData<T> set(C<T> v, String value) throws Exception {
		return root.set(v, value);		
	}

	public <T> Config setV(C<T> t, String value) throws Exception {
		root.set(t, value);
		return this;
	}
	
	public <T> Config setV(C<T> t, T value) {
		root.set(t, value);
		return this;
	}
	
	public <T> CData<T> set(C<T> t, T value) {
		return root.set(t, value);
	}	

	public void clear() {
		root.data.clear();
		root.subs.clear();
		root.errors.clear();
	}
	
	public Config add(Group sub) {
		root.checkValidGroup(sub);
		MyGroupData r;
		root.subs.add(r = new MyGroupData(sub));
		return new Config(r);
	}
	
	public Iterable<Config> subs() {
		return new Iterable<Config>() {
			@Override
			public Iterator<Config> iterator() {
				return new Iterator<Config>() {
					final Iterator<MyGroupData> sit = root.subs.iterator();
					@Override
					public boolean hasNext() {
						return sit.hasNext();
					}

					@Override
					public Config next() {
						return new Config(sit.next());
					}

					@Override
					public void remove() {
						sit.remove();
					}
				};
			}
		};
	}
	
	public int numSub(final Group sub) {
		return root.numSub(sub);
	}
	
	public Config sub(final Group sub, final int idx) {
		Iterator<Config> c = subs(sub).iterator();
		int ii = 0;
		while(c.hasNext()) {
			Config t = c.next();
			if(ii == idx) {				
				return t;
			} 
			ii++;
		}
		throw new IllegalArgumentException("Requested group index " + idx + ", max index is " + (numSub(sub) + 1));		
	}
	
	public Iterable<Config> subs(final Group sub) {
		root.checkValidGroup(sub);
		return new Iterable<Config>() {
			@Override
			public Iterator<Config> iterator() {
				return new Iterator<Config>() {
					Iterator<MyGroupData> sit = root.subIterator(sub);
					@Override
					public boolean hasNext() {
						return sit.hasNext();
					}

					@Override
					public Config next() {
						checkArgument(sit.hasNext());
						return new Config(sit.next());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();						
					}
				};
			}			
		};
	}
	
	public boolean validate() {
		return validate(new StringBuilder());
	}

	static String fill(char c, int cnt) {
		if(cnt > 0) {
			char[] nl = new char[cnt];
			Arrays.fill(nl, c);
			return new String(nl);
		} 
		return "";
	}
	
	public String dump() {
		final StringBuilder b = new StringBuilder();
		root.walk("","", new Walker() {
			@Override
			public void walk(String prepend, String postpend, MyGroupData data) {
				String pname = data.group.name + postpend;
				b.append(fill(' ', prepend.length() - 1) + "." + pname + "\n");
				String p = fill(' ', prepend.length() + pname.length());
				for(CData<?> d : data.data) {
					b.append(p + "." + d.type.name + " = " + d.valueAsString() + "\n");
				}
			}
		});
		return b.toString();
		
	}

	public boolean validate(final StringBuilder b) {
		final class Wrap {
			Boolean wrapped = true; 
		};
		final Wrap wrap = new Wrap();
		root.walk("", "", new Walker() {
			@Override
			public void walk(String prepath, String postpend, MyGroupData data) {
				if(!data.validate()) {
					if(prepath.length() > 0) {
						b.append(prepath); 
					}
					b.append(data.group.name + postpend + " has errors: ");					
					for(String s : data.errors) {
						b.append("\n   ");
						b.append(s);
					}
					b.append("\n");
					wrap.wrapped = false;
				}
			}			
		});
		return wrap.wrapped;
	}

	public Config sub(Group conf) {
		checkArgument(!root().group().relation(conf.name).flag().repeatable, "sub(%s) can only be used on non-repeatable groups", conf.name);
		checkArgument(subs(conf).iterator().hasNext(), "there is no subgroup %s", conf.name);
		return subs(conf).iterator().next();
	}
	
	public Config sub(List<String> path) {
		if(path.size() == 0) {
			return this;
		}
		final String name = path.remove(0);
		final Dependency dep = root.group.relation(name);
		int idx = 0;
		if(dep.flag().repeatable) {
			idx = Integer.parseInt(path.remove(0));			
		}
		return sub(dep.group(), idx);
	}

	public boolean isset(Group conf) {
		return numSub(conf) > 0;
	}

	public boolean isRoot(Group conf) {
		return conf == root.group;
	}
	
	public void setDelayedLoader(DelayedLoader loader) {
		root.setDelayedLoader(loader);
	}
	
	public boolean load(LocalDate date) throws Exception {
		return root.load(date);
	}

}
