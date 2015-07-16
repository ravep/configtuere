package com.unit16.common;

import java.io.File;
import java.nio.file.Path;

import org.joda.time.DateTimeZone;

import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.GroupDef;
import com.unit16.r.onion.util.Clock;

public class Files {

	public static File fromPath(String path) {
		if(path.startsWith("~/")) {
			return new File(System.getProperty("user.home"), path.substring(1));
		}
		return new File(path);
	}
	
	public static void mkdirs(File p) {
		Path h = p.toPath().getParent();
		try {
			java.nio.file.Files.createDirectories(h);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class AppendDate {
	
		public static final GroupDef CONF = GroupDef.create("appendDate").build();		
		private static final C<Boolean> APPEND_DATE = CONF.bool("append_date").Default(Boolean.TRUE).build();
		private static final C<DateTimeZone> TIMEZONE = CONF.timeZone("timezone").Default(DateTimeZone.UTC).build();
		private static final C<String> DATE_PATTERN = CONF.string("date_pattern").Default("YYYY-MM-dd").build();
	
		private final Config conf;
		private final String base;
		
		public AppendDate(Config c, String base_) {
			conf = c;
			base = base_;
		}
		
		public File create(Clock clock, boolean mkdir) {
			String name = base;
			if(conf.get(APPEND_DATE)) {
				name += clock.dateTime(conf.get(TIMEZONE)).toString(conf.get(DATE_PATTERN));				
			}
			File f = fromPath(name);
			if(mkdir) {
				Path p = f.toPath().getParent();
				try {
					java.nio.file.Files.createDirectories(p);
				} catch(Exception e) {
					e.printStackTrace();
				}
			};
			return f;
		}

		public String name() {
			return base.replace('.', '_').replace('/', '_');
		}

		
	}
}
