package com.unit16.z.time;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;

import org.joda.time.LocalTime;

import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.Group.Checker;
import com.unit16.conf.GroupDef;

public class TimeRange {

	public final static GroupDef CONF = GroupDef.create("times")
		.check(new Checker.I() {
			@Override
			public boolean validate(Config c) {
				if(c.get(START).isEqual(c.get(STOP))) {
					setError("Start and stop times are equal.");
					return false;
				}
				return true;
			}
		})
		.build();
	
	public static String noOverlap(Iterable<Config> timeRangeConfig) {
		ArrayList<TimeRange> tr = new ArrayList<>();
		int kk = 1;
		for(Config c : timeRangeConfig) {
			if(!c.validate()) {
				return "Time range " + kk + " is not valid!";
			}
			kk++;
			tr.add(TimeRange.create(c));
		}
		for(int ii = 0; ii < tr.size(); ii++) {
			for(int jj = ii + 1; jj < tr.size(); jj++) {
				if(tr.get(ii).overlaps(tr.get(jj))) {
					return "Time range " + tr.get(ii) + " overlaps " + tr.get(jj);
				}
			}
		}
		return null;
	}
	
	public static final C<LocalTime> START = CONF.localTime("start").build();
	public static final C<LocalTime> STOP  = CONF.localTime("stop").build();
	
	final LocalTime start;
	final LocalTime stop;
	
	public TimeRange(LocalTime start_, LocalTime stop_) {
		start = start_;
		stop = stop_;
		checkArgument(!start.isEqual(stop), "start and stop times are equal?");
	}
	
	public LocalTime start() {
		return start;
	}
	
	public LocalTime stop() {
		return stop;
	}
	
	public static TimeRange create(Config c) {
		return new TimeRange(c.get(START), c.get(STOP));
	}
	
	@Override
	public String toString() {
		return String.format("%s to %s", start.toString(), stop.toString());
	}
	
	public boolean inPeriod(LocalTime t) {
		assert !start.isEqual(stop);
		if(start.isBefore(stop)) {
			return isBeforeOrEqual(start, t) && isBeforeOrEqual(t, stop);
		}
		else {
			return isBeforeOrEqual(t, stop) || isBeforeOrEqual(start, t);
		}
	}	
	
	private static boolean isBeforeOrEqual(LocalTime first, LocalTime second) {
		return first.isBefore(second) || first.isEqual(second);
	}

	public boolean overlaps(TimeRange o) {
		return inPeriod(o.start)
			|| inPeriod(o.stop)
			|| o.inPeriod(start)
			|| o.inPeriod(stop);
	}
	
}
