package org.mytvstream.producerserver;

import com.flazr.rtmp.server.RtmpServer;

public class RTMPProducerServer extends ProducerServer {
	
	
	public boolean canServe(String url) 
	{
		return url.startsWith("rtmp");
	}
	
	public void run() {
		final String orgName = Thread.currentThread().getName();
        	Thread.currentThread().setName(orgName + " - RTMP Thread");
        
		try {			
			RtmpServer.main(null);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
