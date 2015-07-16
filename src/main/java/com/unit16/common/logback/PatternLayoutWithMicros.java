package com.unit16.common.logback;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;

public class PatternLayoutWithMicros extends PatternLayout {
	
	public Map<String, String> getDefaultConverterMap() {
		HashMap<String, String> copy = new HashMap<String,String>(super.getDefaultConverterMap());
		copy.put("micros", MicrosConverter.class.getName());
		return copy;
	}

}
