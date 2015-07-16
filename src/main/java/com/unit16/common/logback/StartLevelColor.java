package com.unit16.common.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;


public class StartLevelColor extends ClassicConverter {
	
	final static String ERROR = "\u001b[0;31m";
	final static String WARN = "\u001b[0;33m";
	final static String DEBUG = "\u001b[0;34m";
	
    @Override
    public String convert(ILoggingEvent event) {
        switch(event.getLevel().toInt()) {
        case Level.ERROR_INT:
            return ERROR;

        case Level.WARN_INT:
            return WARN;
            
        case Level.DEBUG_INT:
        	return DEBUG;
        }       
        return "";
    }

}
