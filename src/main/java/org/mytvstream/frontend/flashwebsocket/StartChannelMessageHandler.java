package org.mytvstream.frontend.flashwebsocket;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;
import org.mytvstream.backend.Backend;
import org.mytvstream.backend.BackendException;
import org.mytvstream.backend.BackendListener;
import org.mytvstream.backend.Channel;
import org.mytvstream.configuration.Configuration;
import org.mytvstream.converter.Converter;
import org.mytvstream.converter.ConverterCodecEnum;
import org.mytvstream.converter.ConverterException;
import org.mytvstream.converter.ConverterFactory;
import org.mytvstream.converter.ConverterFormatEnum;
import org.mytvstream.converter.WebMConverter;
import org.mytvstream.converter.XugglerConverter;
import org.mytvstream.converter.XugglerConverter2;
import org.mytvstream.main.Main;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class StartChannelMessageHandler extends MessageHandler {

	private Logger logger = LoggerFactory.getLogger(StartChannelMessageHandler.class);
	
	private static int CONVERTER_WAIT_TIMEOUT = 2000;
	
	protected Converter converter = new XugglerConverter();
			
	@Override
	protected boolean doHandle(JSONObject object) {
		// TODO Auto-generated method stub
		
		String action = (String)object.get("action");		
		
		if (action.equals("CHANNELSTART")) {
			
			logger.debug("Got CHANNELSTART message");
			
			String format = (String)object.get("format");
			long backendNr = (Long)object.get("backend");
			long bouquetNr = (Long)object.get("bouquet");
			long channelNr = (Long)object.get("channel");					
			String rtmp_url = (String)object.get("rtmpurl");
			String rtmp_stream = (String)object.get("rtmpstream");

			long port = ((int)(Math.random() * 10000)) + 1024; 
			
			Backend backend = Main.getInstance().getBackend().get((int)backendNr);
			
			
			try {
				logger.debug("Get Channel");
				
				Channel channel = backend.getChannelByID(backend.getBouquetByID((int)bouquetNr), (int)channelNr);
				
				Configuration configuration = Main.getInstance().getConfiguration();
				
				logger.debug("Get Channel URL");
				
				String inputUrl = backend.getChannelUrl(new BackendListener() {
					@Override
					public void onMessage(String message) {
						// TODO Auto-generated method stub
						sendChannelMessage(message);
					}
					
				}, channel);
				
				logger.debug("tune Channel");
				
				backend.tuneChannel(new BackendListener() {
					@Override
					public void onMessage(String message) {
						// TODO Auto-generated method stub
						sendChannelMessage(message);
					}
					
				}, channel);
											
				logger.debug("Channel url is " + inputUrl);					
				String outputUrl = rtmp_url + "/" + rtmp_stream;
			
				logger.debug("Channel output url is " + outputUrl);							
			
				converter.close();
				
				if (format.equals("FLV")) { 
					
					logger.debug("opening xuggler converter");
					
					converter = new XugglerConverter();
				
					converter.openMedia(inputUrl, backend.getDefaultFormat());
				
					converter.openOutput(outputUrl, ConverterFormatEnum.valueOf(format));
				
					converter.setupReadStreams("fre");					
				
					ConverterCodecEnum audiocodec = ConverterCodecEnum.MP3;
					ConverterCodecEnum videocodec = ConverterCodecEnum.H264;
					if (configuration.getClient().getAudiocodec().equals("flv")) {
						videocodec = ConverterCodecEnum.FLV1;
					}
					if (configuration.getClient().getAudiocodec().equals("aac")) {
						videocodec = ConverterCodecEnum.AAC;
					}
				
					converter.setupWriteStreams(
						videocodec, 
						configuration.getClient().getVideobitrate().intValue(), 
						audiocodec, 
						configuration.getClient().getAudiobitrate().intValue()
					);
					
					converter.start();
				}	
				
				else if (format.equals("WEBM")){
					
					logger.debug("opening webm converter on port:" +port);
					
					final XugglerConverter2 converter2 = new XugglerConverter2(); 
					
					String[] args = {
					          inputUrl,
					          "-acodec", "libvorbis",
					          "-asamplerate", "44100",
					          "-abitrate", "48000",
					          "-vcodec", "libvpx",
					          "-vbitrate", "1024000",
					          "-containerformat", "webm",
					          "tcp://localhost:" + port + "?listen=1"
					};
					
					Options options = converter2.defineOptions();
				      // And then parse them.
				    final CommandLine cmdLine;
					try {
						cmdLine = converter2.parseOptions(options, args);
						
						Thread t = new Thread(){
							@Override
							public void run() {
								try {
									converter2.run(cmdLine);
								}
								catch(Exception e) {
									e.printStackTrace();
								}
							}
						};
						
						t.start();
						
						Thread.sleep(2000);
												
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						logger.error("Exception in converter:" + e.getMessage());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    
					
				}
				
				
			} catch (ConverterException e1) {
				// TODO Auto-generated catch block
				logger.error("Converter exception : " + e1.getMessage());
				sendChannelFailedMessage(e1.getMessage());
				converter.close();
				return true;
			} catch (BackendException e) {
				// TODO Auto-generated catch block
				logger.error("Backend exception : " + e.getMessage());
				sendChannelFailedMessage(e.getMessage());
				converter.close();
				return true;
			}			
			
												
			JSONObject obj = new JSONObject();
			obj.put("action", "CHANNELSTARTED");
			obj.put("backend", new Long(backendNr));
			obj.put("bouquet", new Long(bouquetNr));
			obj.put("channel", new Long(channelNr));
			obj.put("stream", rtmp_url);
			obj.put("streamname", rtmp_stream);
			obj.put("port", port);
			sendMessage(obj);
			
			return true;
		}
		
		return false;
		
	}
	
	private void sendChannelFailedMessage(String error) { 			
		JSONObject obj = new JSONObject();
		obj.put("action", "CHANNELFAILED");
		obj.put("error", error);
		sendMessage(obj);
	}
	
	/**
	 * Helper
	 * @param message
	 */
	protected void sendChannelMessage(String message) {
		JSONObject obj = new JSONObject();
		obj.put("action", "CHANNELMESSAGE");
		obj.put("message", message);
		try {
			connection.sendMessage(obj.toJSONString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Failed to send message");
		}			
	}

	@Override
	protected void doCleanup() {
		// TODO Auto-generated method stub
		converter.close();
	}
	

}
