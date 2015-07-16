package com.unit16.common.logback;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluatorBase;


public class RateLimitEventEvaluator extends EventEvaluatorBase<ILoggingEvent> {
    private static String tooManyMsg = "Too many triggered log messages! Will shut up now...";
    private long lastTime;
    private long triggerCount;

    private long rate = 5;
    private long period = 60 * 10 * 1000;
    private Level level = Level.ERROR;

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getLevel() {
        return level.levelStr;
    }

    public void setLevel(String levelStr) {
        level = Level.toLevel(levelStr, Level.ERROR);
    }

    public boolean evaluate(ILoggingEvent event) {
        if(event.getLevel().levelInt < level.levelInt) {
            return false;
        }

        long now = event.getTimeStamp();
        if(now - lastTime > period) {
            triggerCount = 0;
            lastTime = now;
        }
        if(tooManyMsg.equals(event.getMessage())) {
            return true;
        } else if(triggerCount > rate) {
            return false;
        } else if(triggerCount == rate) {
            LoggerFactory
                .getLogger(RateLimitEventEvaluator.class)
                .error(tooManyMsg);
            ++triggerCount;
            return false;
        } else {
            ++triggerCount;
            return true;
        }
    }
}

