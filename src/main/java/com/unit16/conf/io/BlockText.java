package com.unit16.conf.io;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Scanner;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.unit16.conf.C;
import com.unit16.conf.CData;
import com.unit16.conf.Config;
import com.unit16.conf.Config.DelayedLoader;
import com.unit16.conf.Group.Dependency;

public class BlockText {

	final private static Logger log = LoggerFactory.getLogger(BlockText.class);
	
	public static void write(Config conf, String filename) throws Exception {
		try(BufferedWriter w = new BufferedWriter(new FileWriter(filename))) {
			w.write("//blockconf\n");
			write(conf, w, "");
		} catch(Exception e) {
			throw new Exception("Can not write to file " + filename);
		}
	}

	public static void write(Config conf, FileWriter fw) throws Exception {		
		fw.write("//blockconf\n");
		write(conf, fw, "");		
	}
	
	private static void write(Config conf, Writer w, String prefix) throws Exception {
		for(CData<?> data : conf.data()) {
			w.write(prefix);
			w.write(data.type.name);
			w.write(" = ");
			w.write(data.valueAsString());
			w.write("\n");
		}
		for(Config sub : conf.subs()) {
			w.write(prefix + sub.root().group().name() + " {\n");
			write(sub, w, prefix + "    ");
			w.write(prefix + "}\n");
		}
	}
	
	public static void read(Config conf, String filename) throws Exception {
		try {
			String text = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
			parse(conf, text, filename);
		} catch(IOException e) {
			throw new Exception("Could not read " + filename + ":\n" + e.getMessage(), e);
		} catch (Exception e) {			
			throw new Exception("Parse error while reading " + filename + ":\n" + e.getMessage(), e);
		}
	}	

	public static void parse(Config conf, String text, String filename) throws Exception {
		BlockToken token = new BlockToken(conf, filename);
		parse(token, text);
	}
	public static void parse(Token token, String text) throws Exception {
		ArrayDeque<Token> tokens = new ArrayDeque<>();
		int ss = 0;
		int cc = 1;
		int line = 1;
		try {
			while(ss < text.length()) {
				if(text.charAt(ss) == '\n') {
					line++;
					cc = 1;
				} else {
					cc++;
				}
				switch(token.next(text, ss)) {
				case BRANCH:						
					tokens.push(token);
					token = token.branch();
					log.trace("Branching out for token {}", token);					
					ss++;
					break;
				case CLOSE_CONTINUE:
					ss++;
				case CLOSE:
					log.trace("Closing branch of token {}", token);
					if(tokens.size() > 0) {
						token = tokens.pop();							
					} else {
						throw new Exception("Unexpected end of block");
					}				
					break;
				case CONTINUE:
					ss++;
					break;
				case ERROR:
					throw new Exception(token.error());
				}				
			}
			if(tokens.size() > 0) {
				throw new Exception("Premature end while parsing token " + token + " (stack: "
					+ Joiner.on(", ").join(tokens) + ")");
			}
			if(!token.consistent()) {
				throw new Exception("Inconsistent token at end: " + token);
			}
		} catch (Exception e) {
			throw new Exception(String.format("Parse error at char %d, line %d (%s):\n%s",
				cc, line, getLine(text, line), e.getMessage()));
		}
	}
	
	private static String getLine(String text, int idx) {
		int ii = 1;
		try(Scanner s = new Scanner(text)) {
			while(s.hasNextLine()) {
				String tmp = s.nextLine();
				if(ii == idx) {
					return tmp;
				}
				ii++;
			}
			return "INVALID LINE NUMBER";
		}
	}
	
	enum State {
		CONTINUE, BRANCH, CLOSE, CLOSE_CONTINUE, ERROR;
	}
	
	private interface Token {
		
		boolean consistent();
		String error();
		State next(String text, int ss) throws Exception;
		Token branch();
		
		static abstract class I implements Token {
			String err;
			I() {}
			
			@Override
			public String error() {
				return err;
			}
			protected void setError(String err_) {
				err = err_;
			}
			@Override
			public boolean consistent() {
				return false;
			}
		}
		
	}
	
	private static class BlockToken extends Token.I {

		private Token branch;
		private Config config;
		private StringBuilder copy;
		private String path;
		
		BlockToken(Config conf, String path_) {
			config = conf;
			path = path_;
			copy = new StringBuilder();
			branch = null;			
		}
		
		@Override
		public boolean consistent() {
			return copy.length() == 0;
		}

		@Override
		public State next(String text, int ss) throws Exception {
			char cc = text.charAt(ss);
			if(cc == '{' || cc == '=') {
				if(name().length() == 0) {
					setError("Unexpected character " + cc + ". Expected to receive name first.");
					return State.ERROR;
				}
				if(cc == '{') {
					branch = new BlockToken(config.add(config.root().group().relation(name()).group()), path);	
				} else {
					branch = new ValueToken(config, name());
				}				
				return State.BRANCH;
			}
			else if(cc == '}') {
				if(name().length() > 0) {
					setError("Unexpected character " + cc + ". Expected to receive definition for name " + name());
					return State.ERROR;
				}
				return State.CLOSE_CONTINUE;
			}
			else {					
				// check for comment
				Token comment = commentToken(text, ss);
				if(comment != null) {
					branch = comment;
					return State.BRANCH;
				}
				if(isWhitespace(cc)) {
					if(copy.length() > 0) {
						copy.append(cc);
					}
					return State.CONTINUE;
				} else {
					if(copy.length() > 0
						&& !isWhitespace(cc)
						&& isWhitespace(copy.charAt(copy.length() - 1))) {
						if(name().equals("include")) {
							branch = new IncludeToken(config, cc, path);
							return State.BRANCH;
						} else if(name().equals("include-latest")) {							
							branch = new IncludeLatestToken(config, cc, path);
							return State.BRANCH;
						} else {
							setError("Expected = or { after name(" + copy.toString() + "), got " + cc);
							return State.ERROR;
						}
					}
					copy.append(cc);
					return State.CONTINUE;
				}
			}		
		}
		private static boolean isWhitespace(char cc) {	
			return Character.isWhitespace(cc) || cc == 160;
		}
		
		private String name() {
			return copy.toString().trim();
		}

		@Override
		public Token branch() {
			copy = new StringBuilder();
			return branch;
		}
		
		@Override
		public String toString() {
			return String.format("NameToken(name=(%s),group=%s)", name(), config.root().group().name());
		}
		
	}	
	
	private static class ValueToken extends Token.I {

		final C<?> type;
		final Config conf;
		final StringBuilder copy = new StringBuilder();
		Token branch;
		
		ValueToken(Config config, String name) throws Exception {
			C<?> tgt = null;
			for(C<?> v : config.root().group().values()) {
				if(v.name.equals(name)) {
					tgt = v;
					break;
				}
			}
			if(tgt == null) {
				throw new Exception("No such value '" + name + "' within group " + config.root().group().name() + "." 
						+ itemText("values", Iterables.transform(config.root().group().values(),
							new Function<C<?>,String>() {
								@Override public String apply(C<?> input) {
									return input.name;
								}	
						}))
						+ itemText("subgroups", Iterables.transform(config.root().group().relations(), 
								new Function<Dependency,String>() {
							@Override public String apply(Dependency input) {
								return input.group().name() + "(" + input.flag() + ")";
							}
						})));
			}
			type = tgt;
			conf = config;
		}

		@Override
		public State next(String text, int ss) throws Exception {
			char cc = text.charAt(ss);
			char pp = ss > 0 ? text.charAt(ss-1) : 0;
			char nn = ss + 1 < text.length() ? text.charAt(ss+1) : 0;
			if(cc == '\n' || (cc == '}' && pp != '\\') || cc == ';') {
				conf.set(type, copy.toString().trim());
				return cc == '}' ? State.CLOSE : State.CLOSE_CONTINUE;
			} else {
				Token comment = commentToken(text, ss);
				if(comment != null) {
					branch = comment;
					return State.BRANCH;
				}
				if(cc != '\\' || (cc == '\\' && (nn != '{' && nn != '}'))) {
					copy.append(cc);
				}
				return State.CONTINUE;
			}
		}

		@Override
		public Token branch() {
			return branch;
		}
		@Override
		public String toString() {
			return String.format("ValueToken(name=(%s),value=%s,group=%s)", type.name, copy.toString().trim(), conf.root().group().name()); 
		}
	}
	
	private static Token commentToken(String text, int ss) {
		if(ss + 1 == text.length() || text.charAt(ss) != '/') {
			return null;
		}
		switch(text.charAt(ss+1)) {
		case '*':
			return new TerminateToken("*/");
		case '/':
			return new TerminateToken("\n");
		default:
			return null;
		}
		
	}
	
	private static class TerminateToken extends Token.I {

		final private String str;
		
		TerminateToken(String token) {
			str = token;
		}

		@Override
		public State next(String text, int ss) throws Exception {
			int ff = ss - str.length() + 1;
			final String cmp = text.substring(ff,ss+1);
			checkArgument(cmp.length() == str.length());
			if(cmp.equals(str)) {
				return State.CLOSE_CONTINUE;
			} else {
				return State.CONTINUE;
			}			
		}

		@Override
		public Token branch() {
			throw new UnsupportedOperationException("I will never branch");
		}
		
		@Override
		public String toString() {
			return String.format("Comment(str=%s)", str.equals("\n") ? "<newline>" : str); 
		}
		
	}
	
	private static class IncludeToken extends Token.I {
		StringBuilder copy = new StringBuilder();
		final Config conf;
		final String path;
		Token branch;
		IncludeToken(Config conf_, char c, String path_) {
			copy.append(c);
			conf = conf_;
			path = path_;
		}	
		@Override
		public State next(String text, int ss) throws Exception {
			char cc = text.charAt(ss);
			if(cc == '\n') {
				String fname = relativePath(path, copy.toString().trim());
				load(fname);
				return State.CLOSE_CONTINUE;
			} else {
				Token tk = commentToken(text, ss);
				if(tk != null) {
					branch = tk;
					return State.BRANCH;
				}
				copy.append(cc);
				return State.CONTINUE;
			}			
		}
		protected void load(String fname) throws Exception {
			log.debug("Including file {}", fname);
			read(conf, fname);
		}
		@Override
		public Token branch() {
			return branch;
		}
		
		@Override
		public String toString() {
			return String.format("Include(path=%s)", copy.toString().trim()); 
		}
		private static String relativePath(String base, String myfile) {
			if(myfile.startsWith("/")) {
				return myfile;
			}
			File f = new File(base);
			File t = new File(f.getParent(), myfile);
			return t.getAbsolutePath();
		}					
	}	
	
	private static class IncludeLatestToken extends IncludeToken {

		IncludeLatestToken(Config conf_, char c, String path_) {
			super(conf_, c, path_);
		}
		
		protected void load(final String fname) throws Exception {			
			log.debug("Installing delayed loading of latest file {}", fname);
			conf.setDelayedLoader(new DelayedLoader() {
				
				String lastLoaded = "";
				
				@Override
				public void load(LocalDate date) throws Exception {					
					for(int tt = 0; tt < 1000; tt++) {
						File f = new File(fname + "." + date.toString());
						if(f.exists()) {
							if(lastLoaded.equals(f.getAbsolutePath())) {
								log.debug("Not loading config from dated file {}. Already done", f.getAbsolutePath());
							} else {
								conf.clear();
								lastLoaded = f.getAbsolutePath();
								log.debug("Loading config from dated file {}", lastLoaded);
								read(conf, lastLoaded);								
							}
							return;
						}
						date = date.minusDays(1);
					}
					throw new Exception("No file found for " + fname);
				}
			});
		}
		
	}

	private static String itemText(String name, Iterable<String> lst) {
		if(lst.iterator().hasNext()) {
			return " Valid " + name + " are (" + Joiner.on(", ").join(lst) + ")."; 
		} else {
			return " No " + name + ".";
		}
	}

}
