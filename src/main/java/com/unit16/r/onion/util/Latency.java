package com.unit16.r.onion.util;

public interface Latency {
	
	String name();
	void add(long nanolatency);
	double min();
	double max();
	double avg();	
	
	public static class I implements Latency {
		
		private final String name;
		private final long[] values;
		private final int sampleSize;
		private long last;
		
		double avg;
		long min;
		long max;
		long curMin;
		long curMax;
		int idx;
		
		public I(String name_, int sampleSize_) {
			name = name_;
			sampleSize = sampleSize_ + 1;
			last = 0;
			idx = 1;
			values = new long[sampleSize];			
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public void add(long nanolatency) {
			if(last == 0) {
				min = max = curMin = curMax = nanolatency;
			} else {
				if(idx == sampleSize - 1) {
					min = curMin;
					max = curMax;
					curMin = curMax = nanolatency;					
				} else {
					curMin = Math.min(curMin, nanolatency);
					curMax = Math.max(curMax, nanolatency);
				}
			}
			last += nanolatency;
			values[idx] = last;
			idx = (idx + 1) % sampleSize;
		}
		
		@Override
		public double min() {
			return Math.min(min, curMin);
		}

		@Override
		public double max() {
			return Math.max(max, curMax);
		}

		@Override
		public double avg() {
			int pre = idx == 0 ? sampleSize - 1 : idx - 1;
			return (values[pre] - values[idx]) / (sampleSize - 1);
		}
				
	}

	
}
