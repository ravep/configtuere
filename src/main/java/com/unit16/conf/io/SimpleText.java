package com.unit16.conf.io;
/**
 * 
 * @author Ratko Veprek
 *  
 * copyright 2013 unit16 atlantic gmbh
 * 
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Scanner;

import com.unit16.conf.C;
import com.unit16.conf.CData;
import com.unit16.conf.Config;
import com.unit16.conf.Group;
import com.unit16.conf.Group.Dependency;

public class SimpleText {
	
	public static Config read(Group root, String filename) throws FileNotFoundException, ParseException {
		return read(new Config(root), filename);
	}
	
	public static Config read(Config c, String filename) throws FileNotFoundException, ParseException {		
		int lineIndex = 1;
		try(Scanner s = new Scanner(new File(filename))) {
			while(s.hasNextLine()) {					
				String line = s.nextLine().trim();
				// backwards compatibility
				if(lineIndex == 1 && line.startsWith("//blockconf")) {
					BlockText.read(c, filename);
					return c;
				}
				if(line.startsWith("include(") && line.endsWith(")")) {
					String incfile = relativePath(filename, line.substring(8, line.length() - 1));
					if((new File(incfile)).exists()) {
						read(c, incfile);
					} else {
						throw new FileNotFoundException("Include file " + incfile + " does not exist");
					}
				} else {
					if(line.length() > 0 && line.charAt(0) != '#') {
						readLine(c, line);
					}
				}
				lineIndex++;
			}
		} catch(ParseException e) {
			throw new ParseException(e.getMessage() + ", file=" + filename + ":" + lineIndex, e.getErrorOffset());
		} catch(Exception e) {
			throw new ParseException(e.getMessage() + ", file=" + filename + ":" + lineIndex, 0);
		}
		return c;
	}
	
	private static String relativePath(String base, String myfile) {
		if(myfile.startsWith("/")) {
			return myfile;
		}
		File f = new File(base);
		File t = new File(f.getParent(), myfile);
		System.out.println("FROM " + base + " via " + f.getParent() + " to " + t.getAbsolutePath());
		return t.getAbsolutePath();
	}
	
	public static void readLines(Config c, String lines) throws Exception {
		for(String l : lines.split("\n")) {
			l = l.trim();
			if(l.length() > 0 && l.charAt(0) != '#') {
				readLine(c, l);
			}
		}
	}

	public static void readLine(Config c, String line) throws Exception {
		int idx = line.indexOf("=");
		if(idx == -1) {
			subConfig(c, line.trim().split("\\."));
		} else {
			String key = line.substring(0, idx).trim();
			String value = line.substring(idx + 1).trim();		
			setValue(c, key, value);
		}
	}
	
	public static Config subConfig(Config c, String[] path)  throws Exception {		
		// find right subgroup
		for(int ii = 0; ii < path.length; ii++) {
			// iterate over all subgroups
			Dependency b = c.root().group().relation(path[ii]);				
			int mIdx = c.numSub(b.group());
			int idx = 0;
			if(b.flag().repeatable) {
				if(ii == path.length - 1) {
					throw new ParseException("Last group " + path[ii] + " is repeatable, but I am missing an index", ii);
				} else {
					try {
						idx = Integer.parseInt(path[ii + 1]);
					} catch(NumberFormatException e) {
						throw new ParseException("Expected integer index, found '" + path[ii + 1] + "'", ii); 
					}
					ii++;
				}
			}
			// get or create groups
			if(idx >= mIdx) {
				Config tmp = null;
				for(int kk = mIdx; kk < idx + 1; kk++) {
					tmp = c.add(b.group());	
				}
				c = tmp;						
			} else {
				c = c.sub(b.group(), idx);
			}				
		}
		return c;
	}
	
	public static void setValue(Config c, String key, String value) throws Exception {		
		String[] s = key.split("\\.");
		c = subConfig(c, Arrays.copyOfRange(s, 0, s.length - 1));		
		C<?> v = c.root().group().value(s[s.length - 1]);
		if(c.isset(v)) {
			throw new Exception("Value for " + key + " has already been set.");
		}
		c.set(v, value);		
	}

	public static void write(Config config, FileWriter fw) throws Exception {		
		PrintWriter w = new PrintWriter(new BufferedWriter(fw));
		try {
			write(true, "", "", config, w);			
		} finally {
			w.close();
		}
	}

	public static void write(Config config, String filename) throws Exception {
		write(config, new FileWriter(filename));
	}
	
	private static void write(boolean excludeRoot, String prepend, String postpend, Config config, PrintWriter w) {		
		String name = prepend;
		if(!excludeRoot) {
			name += config.root().group().name() + ".";	
		}
		name += postpend;
		
		for(CData<?> d : config.data()) {
			w.write(name + d.type.name + " = " + d.valueAsString() + "\n");			
		}
		for(Dependency dep : config.root().group().relations()) {
			int idx = 0;			
			for(Config sub : config.subs(dep.group())) {
				String post = "";
				if(dep.flag().repeatable) {
					post = idx + ".";
				}
				write(false, name, post, sub, w);
				idx++;
			}
		}
		
	}

	
}
