package org.mytvstream.flashwebsocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The message handler is responsible for handling a particullar message from the flash client websocket.
 * @author cbrunner
 *
 */
public abstract class MessageHandler {

	private MessageHandler next;
	protected Connection connection;
	private Logger logger = LoggerFactory.getLogger(MessageHandler.class);
	
	/**
	 * Set the next handler in the chain.
	 * @param next
	 * @return
	 */
	public MessageHandler setNext(MessageHandler next) {
		this.next = next;
		return this;
	}
	
	protected void setConnection(Connection connection) {
		this.connection = connection;
		if (this.next != null) {
			this.next.setConnection(connection);		
		}
	}
	
	
	public boolean handle(JSONObject object) {
		
		if (doHandle(object) == true) {
			return true;
		}
		else if (this.next != null) {
			return this.next.doHandle(object);
		}
		return false;
	}
	abstract protected boolean doHandle(JSONObject object);
	
	public void cleanup() {
		doCleanup();
		
		if (this.next != null) {
			this.next.doCleanup();
		}
	}
	abstract protected void doCleanup();	
	
	protected void sendMessage(JSONObject obj) {
		try{	
			if (connection != null) {
				connection.sendMessage(obj.toJSONString());
			}
		}
		catch (IOException e) {			
			logger.error("Web socket Communication error:" + e.getMessage());
		}
	}
	
}
