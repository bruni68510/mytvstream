package org.mytvstream.frontend.flashwebsocket;

import org.json.simple.JSONObject;
import org.mytvstream.backend.Backend;
import org.mytvstream.backend.BackendException;
import org.mytvstream.backend.Channel;
import org.mytvstream.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPGQueryMessageHandler extends MessageHandler {

	private Logger logger  = LoggerFactory.getLogger(EPGQueryMessageHandler.class);
	
	@Override
	protected boolean doHandle(JSONObject object) {
		// TODO Auto-generated method stub
		
		String action = (String)object.get("action");
		
		if (action.equals("EPG_QUERY")) {
			
			try {
				logger.debug("Got EPG_QUERY message");
			
				long backendNr = (Long)object.get("backend");
				long bouquetNr = (Long)object.get("bouquet");
				long channelNr = (Long)object.get("channel");
			
				Backend backend = Main.getInstance().getBackend().get((int)backendNr);
				
				Channel channel = backend.getChannelByID(backend.getBouquetByID((int)bouquetNr), (int)channelNr);
									
				logger.debug("epg event : {} ", channel.getEPGEvent());
				
				if (channel.hasEPGEvent()) {
					JSONObject obj = new JSONObject();
					obj.put("action", "EPG_QUERY_RESULT");
					obj.put("backend", new Long(backendNr));
					obj.put("bouquet", new Long(bouquetNr));
					obj.put("channel", new Long(channelNr));
					obj.put("epgevent", channel.getEPGEvent().toString().replaceAll("\n", "<br/>"));
										
					sendMessage(obj);
				}
			}
			catch(BackendException e) {
				logger.error(e.getMessage());
				//sendFailedMessage(e.getMessage());
				
				return true;
			}			
			return true;
		}
	
		
		return false;
	}

	@Override
	protected void doCleanup() {}
	

}
