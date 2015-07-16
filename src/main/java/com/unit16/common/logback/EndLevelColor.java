package com.unit16.common.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class EndLevelColor extends ClassicConverter {
	
    @Override
    public String convert(ILoggingEvent event) {
        return "\u001b[0m";
    }

}

