package com.unit16.conf;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public interface Parser<T> {

	T fromString(String s) throws Exception;
	String toString(T t);
	
	public static final class RegExpParser<T> implements Parser<T> {

		final Parser<T> comp;
		final Pattern pattern;
		final String regexp;
		
		public RegExpParser(Parser<T> comp_, String regexp_) {
			comp = comp_;
			regexp = regexp_;
			pattern = Pattern.compile(regexp);
		}		
		
		@Override
		public T fromString(String s) throws Exception {
			if(pattern.matcher(s.trim()).matches()) {
				return comp.fromString(s);
			}
			throw new Exception("Value '" + s + "' does not match required regexp '" + regexp + "'");
		}

		@Override
		public String toString(T t) {
			return comp.toString(t);
		}
		
	}
	
	public static final class I {
		public static final Parser<Double> DBL = new Parser<Double>() {
			@Override
			public Double fromString(String s) throws Exception {
				return Double.parseDouble(s);
			}

			@Override
			public String toString(Double t) {
				return t.toString();
			}			
		};
		private static <N> String makeString(List<N> g) {
			StringBuilder b = new StringBuilder();
			String delim = "";
			for(N obj : g) {
				b.append(delim);
				b.append(obj.toString());
				delim = ", ";
			}
			return b.toString();
		}
		private static <N> List<N> makeList(String s,
				ArrayList<N> target, Parser<N> parser) throws Exception {
			for(String sp : s.split(",")) {
				target.add(parser.fromString(sp));
			}
			return target;
		}
		public static final Parser<List<Double>> LDBL = new Parser<List<Double>>() {
			@Override
			public String toString(List<Double> t) {
				return makeString(t);
			}
			@Override
			public List<Double> fromString(String s) throws Exception {
				return makeList(s, new ArrayList<Double>(), DBL);
			}
		};
		public static final Parser<Integer> INT = new Parser<Integer>() {
			@Override
			public Integer fromString(String s) throws Exception {
				return Integer.parseInt(s);
			}
			@Override
			public String toString(Integer t) {
				return t.toString();
			}
		};
		public static final Parser<List<Integer>> LINT = new Parser<List<Integer>>() {
			@Override
			public String toString(List<Integer> t) {
				return makeString(t);
			}
			@Override
			public List<Integer> fromString(String s) throws Exception {
				return makeList(s, new ArrayList<Integer>(), INT);
			}
		};
		public static final Parser<List<String>> LSTR = new Parser<List<String>>() {
			
			private final Splitter sp = Splitter.on(",");
			
			@Override
			public String toString(List<String> t) {
				return makeString(t);
			}
			@Override
			public List<String> fromString(String s) throws Exception {
				return Lists.newArrayList(sp.split(s));
			}
		};
		public static final Parser<Long> LONG = new Parser<Long>() {
			@Override
			public Long fromString(String s) throws Exception {
				return Long.parseLong(s);
			}
			@Override
			public String toString(Long t) {
				return t.toString();
			}
		};

		public static final Parser<Boolean> BOOL = new Parser<Boolean>() {
			@Override
			public String toString(Boolean t) {
				if(t) {
					return "yes";
				} else {
					return "no";
				}
			}
			@Override
			public Boolean fromString(String s) throws Exception {
				s = s.toUpperCase().trim();
				if(s.equals("YES")) {
					return Boolean.TRUE;
				} else if(s.equals("NO")) {
					return Boolean.FALSE;
				} else {
					throw new Exception("Boolean value '" + s + "' is invalid. yes/no required.");
				}
			}
		};
		public static final Parser<String> STRING = new Parser<String>() {
			@Override
			public String toString(String t) {
				return t.trim();
			}
			@Override
			public String fromString(String s) throws Exception {
				return s.trim();
			}
		};
		public static <T> Parser<T> ENUM(final T[] values) {
			return new Parser<T>() {
				
				@Override
				public T fromString(String s) throws Exception {
					StringBuilder b = new StringBuilder();
					for(T val : values) {
						if(val.toString().equals(s)) {
							return val;
						}
						b.append(" ");
						b.append(val);
					}
					throw new Exception("No such enum: '" + s + "'. Valid options are" + b.toString());
				}

				@Override
				public String toString(T t) {
					return t.toString();
				}
				
			};
		}
		public static Parser<LocalDate> LOCALDATE = new RegExpParser<>(								
			new Parser<LocalDate>() {
				
				final DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd");
				
				@Override
				public LocalDate fromString(String s) throws Exception {
					return LocalDate.parse(s, fmt);
				}
	
				@Override
				public String toString(LocalDate t) {
					return t.toString(fmt);
				}
			}, "^[0-9]{4}-[0-9]{2}-[0-9]{2}$"
		);					
		public static final Parser<LocalTime> LOCALTIME = new RegExpParser<>(
			new Parser<LocalTime>() {
				
				final DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss");
				
				@Override
				public LocalTime fromString(String s) throws Exception {
					return LocalTime.parse(s, fmt);
				}
	
				@Override
				public String toString(LocalTime t) {
					return t.toString(fmt);
				}
			},
			"^[0-9]{1,2}:[0-9]{2}:[0-9]{2}$"
		);
		
		public static final Parser<DateTimeZone> TIMEZONE = new Parser<DateTimeZone>() {
			@Override
			public DateTimeZone fromString(final String s) throws Exception {
				for(String o : DateTimeZone.getAvailableIDs()) {
					if(o.equalsIgnoreCase(s)) {
						return DateTimeZone.forID(s);
					}
				}
				// find valid subchoice
				if(s.length() > 0) {
					List<String> ls = Lists.newArrayList(
						Iterables.filter(DateTimeZone.getAvailableIDs(), new Predicate<String>() {
							@Override
							public boolean apply(String input) {
								return input.toUpperCase().startsWith(s.toUpperCase());
							}
						}));
					if(ls.size() > 0) {
						throw new Exception("Incomplete time zone '" + s + "'. Possible completions: " +
						Joiner.on(", ").join(ls));
					}					
				}	
				String[] sp = s.split("/");
				TreeSet<String> sub = new TreeSet<>();
				TreeSet<String> suf = new TreeSet<>();
				for(String o : DateTimeZone.getAvailableIDs()) {
					sub.add(o.split("/")[0]);
					if(o.startsWith(sp[0])) {
						suf.add(o);
					}
				}
				if(suf.size() > 0) {
					throw new Exception("Invalid time zone '" + s + "'. Options starting with " + sp[0] + " are " + Joiner.on(", ").join(suf));
				} else {
					throw new Exception("Invalid time zone '" + s + "'. Should start with " + Joiner.on(", ").join(sub));
				}
			}
						
			@Override
			public String toString(DateTimeZone t) {
				return t.toString();
			}
			
		};
		
		public static final Parser<File> FILE = new Parser<File>() {
			@Override
			public File fromString(String s) throws Exception {
				int length = 0;
				if(s.startsWith("~/")) {
					length = 2;
				}
				else if(s.startsWith("~")) {
					length = 1;
				}
				if(length > 0) {
					return new File(System.getProperty("user.home"), s.substring(length));
				}
				return new File(s);
			}

			@Override
			public String toString(File t) {
				return t.getPath();
			}
			
		};
		public static final Parser<Pattern> PATTERN = new Parser<Pattern>() {

			@Override
			public Pattern fromString(String s) throws Exception {
				return Pattern.compile(s);
			}

			@Override
			public String toString(Pattern t) {
				return t.pattern();
			}
			
		};
		

	}

		
}
