package com.unit16.z.time;

import static com.google.common.base.Preconditions.checkArgument;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.GroupDef;
import com.unit16.conf.GroupDef.DelayedInit;
import com.unit16.conf.GroupDepInit;
import com.unit16.z.Sequence;

public class DateTimeRange {

	public final static GroupDef CONF = GroupDef.create("range")
		.init(new DelayedInit() {
			@Override public void init(GroupDepInit d) {
				d.required(DateRange.CONF, TimeRange.CONF);
			}
		})
		.build();

	private static final C<DateTimeZone>  TIMEZONE = CONF.timeZone("timezone").Default(DateTimeZone.getDefault()).build();
		
	public static DateTimeRange create(Config c) {
		checkArgument(c.isRoot(CONF));
		
		return new DateTimeRange(
			DateRange.create(c.sub(DateRange.CONF)),
			TimeRange.create(c.sub(TimeRange.CONF)),
			c.get(TIMEZONE)
		);
	}
	
	private final DateRange dateRange;
	private final TimeRange timeRange;
	private final DateTimeZone tz;
		
	public DateTimeRange(DateRange dateRange_, TimeRange timeRange_, DateTimeZone tz_) {
		dateRange = dateRange_;
		timeRange = timeRange_;
		tz = tz_;
	}
	
	public TimeRange range() {
		return timeRange;
	}
	
	public DateTimeZone tz() {
		return tz;
	}
	
	public Sequence<LocalDate> period() {
		return dateRange.period();
	}
	
}
