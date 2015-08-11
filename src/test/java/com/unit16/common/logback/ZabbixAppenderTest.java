package com.unit16.common.logback;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class ZabbixAppenderTest {

	@Test
	public void test() throws InterruptedException {
		
	
		// well ..you need zabbix to test this ..
		
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger log = lc.getLogger("test");

		ZabbixAppender t = new ZabbixAppender();		
		t.setContext(lc);
		log.addAppender(t);
		
		t.setHostname("othpc88.ecintra.net");
		t.setHeartbeatSec(10);
		t.setZabbixKey("logback.test");
		t.setZabbixServer("zabbix.ecintra.net");
		
		t.start();
		
		log.info("NOPE");
		log.error("Serious error");
		
		for(int ii = 0; ii < 1; ii++) {
			log.info("iteration " + ii);
			Thread.sleep(100);
			if(ii % 1 == 0) {
				log.error("sending error " + ii);
			}
			Thread.sleep(900);
		}
		
		t.stop();
		//Thread.sleep(30000);
		//
		 
		
	}

}
