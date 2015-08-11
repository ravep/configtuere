package com.unit16.z.time;

import java.util.Comparator;

public interface GMTNanos {
	
	public long gmtNanos();
	
	public static final class Const extends NicelyFormatted
	{
		private final long ts_;
		public Const(long t) { ts_ = t; }

		@Override
		public long gmtNanos() { return ts_; }
	}
	
	static abstract class NicelyFormatted implements GMTNanos
	{
		@Override
		public String toString()
		{
			final long micros = (gmtNanos() % 1_000_000) / 1000;
			return DateUtils.fromNanos(this).toString() + "." + micros;
		}
	}
	
	public static final Comparator<GMTMicros> ORDERING = new Comparator<GMTMicros>()
	{
		@Override
		public int compare(GMTMicros o1, GMTMicros o2) {
			
			final long dl = o1.gmtMicros() - o2.gmtMicros();
			
			return Long.signum(dl);
		}
	};
}
