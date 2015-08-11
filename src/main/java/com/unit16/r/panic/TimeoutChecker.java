package com.unit16.r.panic;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import com.google.common.collect.Range;
import com.unit16.conf.C;
import com.unit16.conf.Config;
import com.unit16.conf.Group;
import com.unit16.conf.GroupDef;
import com.unit16.conf.GroupDef.DelayedInit;
import com.unit16.conf.GroupDepInit;
import com.unit16.r.onion.util.Clock;
import com.unit16.r.onion.util.Clock.Action;
import com.unit16.z.time.TimeRange;


public interface TimeoutChecker {	    
	
	public interface Listener {
		void timeoutError(long timeoutSinceNanos);
		void timeoutWarning(long timeoutSinceNanos);
		void goodAgain(boolean wasError);
	}
	
	void alive();
	
	public static final TimeoutChecker NONE = new TimeoutChecker() {		
		@Override
		public void alive() {}
	};
	
	public static class I implements TimeoutChecker, Action {
		public static final GroupDef CONF = GroupDef.create("timeout")
			.init(new DelayedInit() {
				@Override
				public void init(GroupDepInit d) {
					d.atleastone(TimeRange.CONF);
				}
			})
			.check(new Group.Checker.I() {
				@Override public boolean validate(Config c) {
					String err = TimeRange.noOverlap(c.subs(TimeRange.CONF));
					if(err != null) {
						setError(err);
						return false;
					}
					return true;
				}			
			})
			.build();
		private static final C<DateTimeZone> TIMEZONE = CONF.timeZone("timezone").Default(DateTimeZone.UTC).build();
		private static final C<Double> TIMEOUT_SECS = CONF.dbl("timeout_secs").check(Range.greaterThan(1.0e-3)).build();
		private static final C<Double> CHECK_FRACTION = CONF.dbl("check_frequency").check(Range.atLeast(1.0)).Default(10.0).build();
		
		public static TimeoutChecker factory(Listener listener, String name, Config c, Clock clock) {
			ArrayList<TimeRange> ranges = new ArrayList<>();
			for(Config s : c.subs(TimeRange.CONF)) {
				ranges.add(TimeRange.create(s));
			}
			long timeoutNanos = Math.round(c.get(TIMEOUT_SECS) * 1e9);
			long check = Math.round(timeoutNanos / c.get(CHECK_FRACTION));
			return new TimeoutChecker.I(listener, name, clock, ranges, c.get(TIMEZONE), timeoutNanos, check);
		}
	    
	    private final DateTimeZone timezone;
	    private final List<TimeRange> ranges;
	    private final long timeoutErrorNanos;
	    private final long checkIntervalNanos;
	    private final Clock clock;
	    private long lastSignOfLife;
	    private boolean errorSent;
	    private boolean warningSent;
	    private final Listener listener;
	    
	    public I(Listener listener_, String name, Clock clock_, List<TimeRange> ranges_, 
	    		DateTimeZone tz, long timeoutErrorNanos_, long checkIntervalNanos_) {	    	
	        timeoutErrorNanos = timeoutErrorNanos_;
	        lastSignOfLife = 0;
	        errorSent = false;
	        warningSent = false;
	        ranges = ranges_;
	        timezone = tz;
	        clock = clock_;
	        listener = listener_;
	        checkIntervalNanos = checkIntervalNanos_;
	        clock.schedule(clock.gmtNanos() + checkIntervalNanos, this);
	        checkArgument(ranges.size() > 0);
	    }
	    
	    public void alive() {
	    	lastSignOfLife = clock.gmtNanos();
	    }
	    
		public long scheduledAction(long now) {
		
	        if(lastSignOfLife > 0) {
	            if(shouldBeRunning(now)) {
		            if((now - lastSignOfLife) > timeoutErrorNanos / 2) {
		                if((now - lastSignOfLife) > timeoutErrorNanos) {
		                    if(!errorSent) {
		                    	listener.timeoutError(now - lastSignOfLife);
		                        errorSent = true;
		                    }
		                } else {
		                    if(!warningSent) {
		                    	listener.timeoutWarning(now - lastSignOfLife);
		                        warningSent = true;
		                    }
		                }
		            }
		            else if(errorSent || warningSent) {
		            	listener.goodAgain(errorSent);
		                warningSent = false;
		                errorSent = false;
		            }
	            }
	        }
	        return now + checkIntervalNanos;
	    }		
	
		private boolean shouldBeRunning(long now) {		
			LocalTime lt = clock.dateTime(timezone).toLocalTime();
			for(TimeRange tz : ranges) {
				if(tz.inPeriod(lt)) {
					return true;
				}
			}
			return false;			
		}
	    
	}
	
}
