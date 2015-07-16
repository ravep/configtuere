package com.unit16.common.logback;

import static com.google.common.base.Preconditions.checkNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MicrosConverter extends ClassicConverter {

	public static void updateTime(long now) {
		gmtNanos = now;
	}
	
	private static volatile long gmtNanos;
	
    private String cachedStr = null;
	private final SimpleDateFormat sdf;
	private final StringBuilder str;
	private long last;
	
	public MicrosConverter() {
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(checkNotNull(TimeZone.getTimeZone("GMT")));
		str = new StringBuilder();
	}
	
	
	@Override
	public String convert(ILoggingEvent event) {
		// takes like 200 nanos on my machine
		if(gmtNanos > 0) {
			long nanos = gmtNanos % 1_000_000_000;		
			long now = gmtNanos - nanos;
			synchronized(this) {
				if(now != last) {
					cachedStr = sdf.format(new Date(now / 1_000_000l));
					last = now;
				}
				str.setLength(0);
				str.append(cachedStr);
				str.append(".");
				long digit = 100_000_000;			
				for(int ii = 0; ii < 6; ii++) {
					long rest = nanos % digit;
					int num = (int)((nanos - rest) / digit);
					assert num >= 0 && num < 10;
					str.append((char)(num + 48));
					nanos = rest;
					digit /= 10;
					if(ii == 2) {
						str.append("'");
					}
				}
				return str.toString();
			}
		} else {
			return "NOTIME";
		}
	}
}
