package org.mytvstream.flashwebsocket;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FlashClientWebSocketServlet extends WebSocketServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8055293947333522981L;

	private static Logger logger = LoggerFactory.getLogger(FlashClientWebSocketServlet.class);
	
	
	
	public WebSocket doWebSocketConnect(HttpServletRequest arg0, String arg1) {
		return new MyTvStreamSocket();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request,response);
	}
	
	class MyTvStreamSocket implements WebSocket.OnTextMessage {
	
		protected MessageHandler handler;		
		
		public MyTvStreamSocket() {									
			
			handler = new StartChannelMessageHandler()
				.setNext(new EPGQueryMessageHandler());			
		}
		
		public synchronized void onMessage(String data) {
			
			try {
				
				JSONParser parser = new JSONParser();
			
				JSONObject jsonObject = (JSONObject) parser.parse(data);
												
				if (!handler.handle(jsonObject)) {
					logger.error("Message from client not understood");
				}
				
			}
			catch(ParseException e) {
				logger.error("Error from json parser:" + e.getMessage());
				return;
			} 
			
		}
		
				
		public void onOpen(Connection connection) {
			handler.setConnection(connection);			
		}		
		
		public void onClose(int closeCode, String message) {
			logger.debug("Closing websocket connection");	
			handler.cleanup();
		}
		
		
	}

}
