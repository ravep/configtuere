package com.unit16.z.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import com.google.common.base.MoreObjects;
import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.GFactory;
import com.unit16.conf.GroupDef;
import com.unit16.r.onion.util.Clock;

public class TimePoint {

	public static GFactory createConf(String name) {
		return CONF.createClone(name);
	}	
	
	public static final GroupDef CONF = GroupDef.create("tm").build();
	private static final C<DateTimeZone> TIMEZONE = CONF.timeZone("timezone").Default(DateTimeZone.UTC).build();
	private static final C<LocalTime> LOCALTIME = CONF.localTime("localtime").build();			
	
	public static TimePoint factory(Config c) {
		return new TimePoint(c.get(TIMEZONE), c.get(LOCALTIME));
	}
	
	public final LocalTime localTime;
	public final DateTimeZone timeZone;
	
	public TimePoint(DateTimeZone dtz, LocalTime lt) {
		timeZone = dtz;
		localTime = lt;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass())
			.add("localTime", localTime)
			.add("timeZone", timeZone)
			.toString();
	}
	
	public DateTime next(Clock c) {
		DateTime dt = c.dateTime(timeZone).withFields(localTime);
		if(dt.isBeforeNow()) {
			return dt.plusDays(1);
		}
		return dt;
	}


}
