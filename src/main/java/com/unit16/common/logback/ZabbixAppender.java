package com.unit16.common.logback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.base.Preconditions;
import com.unit16.z.Pair;

public class ZabbixAppender extends AppenderBase<ILoggingEvent> {
	
	private final Thread runner;
	private final ConcurrentLinkedQueue<Pair<String, String>> queue = new ConcurrentLinkedQueue<>();
	private volatile boolean running = false;
	private final Pair<String,String> heartbeat = Pair.Uniform.pair("HEARTBEAT", "This is just a heartbeat");
	
	private int count = 0;
	private long minute = 0;
	private int ratePerMinute = 5;
	private int heartbeatSec = 120;
	private long lastHeartbeat = 0;
	private String hostname;
	private String zabbixServer;
	private int waitTime = 0;

	private int    zabbixPort = 10051;
	private String zabbixKey;
	private long lastAttempt = 0;
	private SocketChannel channel;
	
	
	public ZabbixAppender() {
		runner = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Started appender");
				while(running) {
					try {
						Thread.sleep(waitTime = Math.min(waitTime + 1, 1000));
					} catch(Exception e) {}
					flush();
				}
				System.out.println("Stopped appender");
			}
		});		
		runner.setName("ZabbixAppender");
		runner.setDaemon(true);
	}
	
	void flush() {
		
		final long now = System.currentTimeMillis();
		
		Pair<String,String> item = queue.poll();
		if(item != null) {
			// reset rate if required
			long curMin = now;
			curMin = curMin - (curMin % (1000 * 60));
			if(curMin != minute) {
				minute = curMin;
				count = 0;
			}
			if(count + 1 > ratePerMinute) {
				queue.clear();
			} else {
				count++;
				send(now, item);
			}
		} else {
			if(heartbeatSec > 0 && now - lastHeartbeat > heartbeatSec * 1000) {
				lastHeartbeat = now;
				queue.add(heartbeat);
			}
		}
	}

	private void send(long now, Pair<String, String> item) {
		connect(now);
		if(channel == null) {
			queue.add(item);
			return;
		}
		try {
			writeMessage(buildJSonString(hostname, zabbixKey, item.fst() + ": " + item.snd()).getBytes());
		} catch(Exception e) {
			
		} finally {
			disconnect();
		}
		
	}
	
	private void connect(long now) {
		if(channel == null && (now - lastAttempt) > 60000) {
			try {
				channel = SocketChannel.open(new InetSocketAddress(zabbixServer, zabbixPort));
			} catch(Exception e) {
				System.out.println("Failed to connect to zabbix!");
				lastAttempt = now;
				e.printStackTrace();
			}
		}
	}
	
	private void disconnect() {
		if(channel != null) {
			try {
				channel.close();
			} catch (IOException e) {}
			channel = null;
		}
	}

	public int getHeartbeatSec() {
		return heartbeatSec;
	}

	public void setHeartbeatSec(int heartbeatSec) {
		this.heartbeatSec = heartbeatSec;
	}	

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getZabbixServer() {
		return zabbixServer;
	}

	public void setZabbixServer(String zabbixServer) {
		this.zabbixServer = zabbixServer;
	}

	public int getZabbixPort() {
		return zabbixPort;
	}

	public void setZabbixPort(int zabbixPort) {
		this.zabbixPort = zabbixPort;
	}

	public String getZabbixKey() {
		return zabbixKey;
	}

	public void setZabbixKey(String zabbixKey) {
		this.zabbixKey = zabbixKey;
	}

	@Override
	protected void append(ILoggingEvent event) {		
		if(event.getLevel().equals(Level.ERROR) && queue.size() < 5) {
			queue.add(Pair.I.pair(event.getLevel().toString(), event.getMessage()));
		}
	}
	
	@Override
	public void start() {
		if(running) {
			throw new RuntimeException("Already running");
		}
		Preconditions.checkArgument(zabbixServer != null, "zabbixServer is missing");
		Preconditions.checkArgument(zabbixKey != null, "zabbixKey is missing");
		Preconditions.checkArgument(hostname != null, "hostname is missing");
		super.start();
		running = true;
		runner.start();
	}
	
	@Override
	public void stop() {
		super.stop();
		running = false;
		runner.interrupt();
		try {
			runner.join(250);
		} catch(Exception e) {}
		disconnect();
	}

	public int getRatePerMinute() {
		return ratePerMinute;
	}

	public void setRatePerMinute(int ratePerMinute) {
		this.ratePerMinute = ratePerMinute;
	}

	private String buildJSonString(String host, String item, String value)
	{
		return 	"{"
        + "\"request\":\"sender data\",\n"
        + "\"data\":[\n"
        +        "{\n"
        +                "\"host\":\"" + host + "\",\n"
        +                "\"key\":\"" + item + "\",\n"
        +                "\"value\":\"" + value.replace("\\", "\\\\") + "\"}]}\n" ;
	}
 
	protected void writeMessage(byte[] data) throws IOException {
		
		int length = data.length;		
		byte[] header = new byte[] {
				'Z', 'B', 'X', 'D', 
				'\1',
				(byte)(length & 0xFF), 
				(byte)((length >> 8) & 0x00FF), 
				(byte)((length >> 16) & 0x0000FF), 
				(byte)((length >> 24) & 0x000000FF),
				'\0','\0','\0','\0'};
				
		ByteBuffer buf = ByteBuffer.allocate(Math.max(4096, data.length + 100));
		buf.put(header, 0, header.length);
		buf.put(data,  0,  data.length);
		
		buf.flip();
		channel.write(buf);
		buf.clear();
		
		channel.read(buf);		
		StringBuilder b = new StringBuilder();
		b.append("read=");
		b.append(buf.position());
		b.append(" bytes: ");
		buf.flip();
		for(int ii = 0; ii < buf.limit(); ii++) {
			Character chr = new Character((char)buf.get(ii));
			if(Character.isLetterOrDigit(chr)) {
				b.append(chr);
			} else {
				b.append('.');
			}
		}
		System.out.println(b.toString());
	}
	
	
}
