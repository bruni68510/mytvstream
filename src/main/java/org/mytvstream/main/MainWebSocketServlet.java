package org.mytvstream.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketServlet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MainWebSocketServlet extends WebSocketServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8055293947333522981L;

	private static Logger logger = LoggerFactory.getLogger(MainWebSocketServlet.class);
	
	private static int CONVERTER_WAIT_TIMEOUT = 2000;
	
	//private static String RMTP_URL = "rtmp://192.168.0.35/flvplayback";
	
	
	
	public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
		return new MyTvStreamSocket();
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request,
				response);
	}
	
	class MyTvStreamSocket implements WebSocket.OnTextMessage {

		protected Converter converter = null;
		protected Connection connection = null;		
		
		private void closeConverter() {
			if (converter != null && converter.isAlive()) {
				
				logger.debug("Closing current converter");
				
				converter.close();
				try {
					converter.join(CONVERTER_WAIT_TIMEOUT);
					Thread.sleep(3000);
					converter = null;
				} catch (InterruptedException e) {
					logger.error("Timeout error waiting for converter to close :" + e.getMessage());
				}
				
			}
		}
		
		public synchronized void onMessage(String data) {
			
			String error = "Message not understood";
			
			try {
				
				JSONParser parser = new JSONParser();
			
				JSONObject jsonObject = (JSONObject) parser.parse(data);
			
				String action = (String)jsonObject.get("action");
				
				
				if (action.equals("CHANNELSTART")) {
					
					logger.debug("Got CHANNELSTART message");
					
					String format = (String)jsonObject.get("format");
					long backendNr = (Long)jsonObject.get("backend");
					long bouquetNr = (Long)jsonObject.get("bouquet");
					long channelNr = (Long)jsonObject.get("channel");					
					String rtmp_url = (String)jsonObject.get("rtmpurl");
					String rtmp_stream = (String)jsonObject.get("rtmpstream");
				
					Backend backend = Main.getInstance().getBackend().get((int)backendNr);
					
					//Channel channel = backend.getBouquets().get((int)bouquetNr).getChannels().get((int)channelNr);
					String inputUrl = backend.getChannelUrl((int)channelNr);
					
					backend.tuneChannel(new BackendListener() {
						@Override
						public void onMessage(String message) {
							// TODO Auto-generated method stub
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
						
					}, inputUrl);
										
					
					logger.debug("Channel url is " + inputUrl);					
					String outputUrl = rtmp_url + "/" + rtmp_stream;
					
					logger.debug("Channel output url is " + outputUrl);
					
					closeConverter();
					
					converter = ConverterFactory.getInstance().getConverter(
						inputUrl,
						backend.getDefaultFormat(),
						outputUrl,
						ConverterFormatEnum.valueOf(format)
					);
					
					converter.setupReadStreams("fre");
					
					Configuration configuration = Main.getInstance().getConfiguration();
					
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
														
					JSONObject obj = new JSONObject();
					obj.put("action", "CHANNELSTARTED");
					obj.put("backend", new Long(backendNr));
					obj.put("bouquet", new Long(bouquetNr));
					obj.put("channel", new Long(channelNr));
					obj.put("stream", rtmp_url);
					obj.put("streamname", rtmp_stream);
					connection.sendMessage(obj.toJSONString());
					
					return;
				}
				
			}
			catch(ParseException e) {
				logger.error("Error from json parser:" + e.getMessage());
				return;
			} catch (ConverterException e) {
				error = "Converter error:" + e.getMessage();
				logger.error(error);
				closeConverter();
				
			} catch (IOException e) {
				logger.error("Web socket Communication error:" + e.getMessage());
				closeConverter();
				return;
			}
			catch (BackendException e) {
				error = e.getMessage();
			}
			
			try{
				JSONObject obj = new JSONObject();
				obj.put("action", "CHANNELFAILED");
				obj.put("error", error);
				connection.sendMessage(obj.toJSONString());
			}
			 catch (IOException e) {

				logger.error("Web socket Communication error:" + e.getMessage());
				closeConverter();
			}
		}
		
				
		public void onOpen(Connection connection) {
			this.connection = connection;
		}		
		
		public void onClose(int closeCode, String message) {
			logger.debug("Closing websocket connection");
			closeConverter();			
		}

	}

}
