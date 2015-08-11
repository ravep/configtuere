package com.unit16.z.time;

import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.unit16.z.Sequence;

public class DateUtils {

	public static Predicate<LocalDate> WEEKDAY = new Predicate<LocalDate>(){

		@Override
		public boolean apply(LocalDate input) {
			final int wd = input.getDayOfWeek();
			return wd < 6;
		}};
	
	/**
	 * @param start
	 * @param end
	 * @return all days between start (inclusive) and end (inclusive) -- empty sequence if start is after end.
	 */
	public static Sequence<LocalDate> period(final LocalDate start, final LocalDate end)
	{
		return new Sequence<LocalDate>(){

			private LocalDate today = start;
			
			@Override
			public LocalDate next() { 
				
				final LocalDate r = today;
				today = r.plusDays(1);
				return r;
			}

			@Override
			public boolean hasNext() {
				return !today.isAfter(end);
			}};
	}
	
	public static DateTime fromNanos(long nanos) {
		return new DateTime(TimeUnit.NANOSECONDS.toMillis(nanos)).withZone(DateTimeZone.UTC);
	}
	
	public static DateTime fromNanos(GMTNanos nanos) {
		return new DateTime(TimeUnit.NANOSECONDS.toMillis(nanos.gmtNanos())).withZone(DateTimeZone.UTC);
	}
	
	// TODO: this sucker is actually quite slow (2 micros for a fucking dumb timestamp)
	public static String fromNanosWithMicros(long nanos) {
		String s = new DateTime(TimeUnit.NANOSECONDS.toMillis(nanos)).withZone(DateTimeZone.UTC).toString();
		return String.format("%s%03d", s, (nanos % 1_000_000) / 1000);
	}
	
	public static DateTime fromMicros(GMTMicros micros)
	{
		return new DateTime(TimeUnit.MICROSECONDS.toMillis(micros.gmtMicros())).withZone(DateTimeZone.UTC);
	}

}
