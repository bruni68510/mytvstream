package org.mytvstream.producerserver;

import com.flazr.rtmp.server.RtmpServer;

public class RTMPProducerServer extends ProducerServer {
	
	
	public boolean canServe(String url) 
	{
		return url.startsWith("rtmp");
	}
	
	public void run() {
		try {			
			RtmpServer.main(null);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
