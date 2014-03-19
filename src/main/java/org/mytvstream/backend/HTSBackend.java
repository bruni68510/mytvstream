package org.mytvstream.backend;

import java.net.ConnectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import org.mytvstream.converter.ConverterFormatEnum;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.tvheadend.tvhguide.htsp.HTSConnection;
import org.tvheadend.tvhguide.htsp.HTSConnectionListener;
import org.tvheadend.tvhguide.htsp.HTSMessage;
import org.tvheadend.tvhguide.htsp.HTSResponseHandler;

public class HTSBackend extends Backend implements HTSConnectionListener, IBackend {

	/**
	 * Backend connection
	 */
	protected HTSConnection connection;
	protected String webroot = new String();
	
	/**
	 * Class logger
	 */
	private static Logger logger = LoggerFactory.getLogger(HTSBackend.class);
	
	/**
	 * Default bouquet
	 */
	protected static String bouquetName = "TV and Radios";
	
	
	/**
	 * Connect to backend using connectionProperties
	 * Following properties are excepted:
	 *  - server, username, password and port
	 * @param connectionProperties
	 */
	public HTSBackend(int id, org.mytvstream.configuration.Configuration.Backends.Backend backendConfiguration) 
	{
	
		super(id,backendConfiguration);
		
		bouquets.add(new Bouquet(0,bouquetName,0));		
		name = "HTS Backend " + backendConfiguration.getServer();
	}

	/**
	 * Copy constructor
	 * @param backend
	 */
	protected HTSBackend() {
		// TODO Auto-generated constructor stub
		super();
	}

	protected void connect(HTSConnectionListener listener) throws BackendException {
		
		connection = new HTSConnection(listener, HTSBackend.class.getPackage().getName(), HTSBackend.class.getName());		
		connection.setDaemon(true);
		connection.open(server, port);
		
		if (connection == null || !connection.isConnected()) {		
			throw new BackendException("Failed to connect to backend");			
		}
		
		connection.authenticate(username, password);
				
		webroot = connection.getWebRoot();
		
		if (webroot == null) {
			webroot = "http://" + 
					username + ":" +
					password + "@" +
					server + ":" +
					httpport;
		}
		
	}
	
	public void connect() throws BackendException {
		connect(this);
	}
	
	/**
	 * The HTS backend has delivered an asynchronous message
	 * See HTSP definition at https://tvheadend.org/projects/tvheadend/wiki/Htsp for the list of possible messages
	 */
	public void onMessage(HTSMessage response) {
		if (response.getMethod().equals("channelAdd")) {
			logger.debug("Adding channel " + response);
			
			long channelID = response.getLong("channelId");
			long eventID = response.getLong("eventId", -1);
			
			// add channel to channel list
	        String ChannelName = response.getString("channelName", null);
	        int channelNumber = response.getInt("channelNumber", 0);
	        
	        if (channelNumber == 0) {
	        	channelNumber = 1000;
	        }
	        
	        Channel newChannel = new Channel(ChannelName, (int)channelID, channelNumber);
	        
	        bouquets.get(0).channels.add(newChannel);
	        Collections.sort(bouquets.get(0).channels);
	        	        
			if (eventID != -1) {
				for (Iterator<Channel> it = bouquets.get(0).channels.iterator(); it.hasNext();){
					Channel channel = it.next();
					if (channel.getID() == channelID){
						EPGEvent event = new EPGEvent(eventID,channel);
						getEPGDetails(event);
						channel.setEPGEvent(event); 
					}
				}			
			}
			
		}
		else if (response.getMethod().equals("channelDelete")) {
			long channelID = response.getLong("channelId");
						
			//channels.remove(new Channel("",(int)channelID,0));
			
			for (Iterator<Channel> it = bouquets.get(0).channels.iterator(); it.hasNext();){
			    Channel channel = it.next();
			    if (channel.getID() == channelID){
			        it.remove();
			    }
			}
		}
		
		else if (response.getMethod().equals("channelUpdate")) {
			long channelID = response.getLong("channelId");
			long eventID = response.getLong("eventId", -1);			
						
			if (eventID != -1) {
				for (Iterator<Channel> it = bouquets.get(0).channels.iterator(); it.hasNext();){
					Channel channel = it.next();
					if (channel.getID() == channelID){
						EPGEvent event = new EPGEvent(eventID,channel);
						getEPGDetails(event);
						channel.setEPGEvent(event); 
					}
				}			
			}
		}
		
		else {
			logger.debug("Got message " + response);
		}
	}

	/**
	 * HTS Backend error handling
	 * @param errorCode : see HTSConnection for definition
	 */
	public void onError(int errorCode) {
		
		if (errorCode == HTSConnection.CONNECTION_LOST_ERROR)		
			logger.error("Connection lost to hts backend ");
		if (errorCode == HTSConnection.CONNECTION_REFUSED_ERROR) {
			logger.error("hts backend refused to connect");
		}
		if (errorCode == HTSConnection.HTS_AUTH_ERROR) 
			logger.error("hts backend refused the credentials supplied");
		if (errorCode == HTSConnection.HTS_MESSAGE_ERROR) 
			logger.error("Got an unexcepted error from hts server");
		
		// remove the current connection the reconnect worker may recreate the connection later
		if (connection != null && connection.isConnected()) {
			connection.close();
		}
		connection = null;
	}

	/**
	 * HTS backend exception handling
	 */
	public void onError(Exception ex) {
		logger.error("Got an exception from hts backend",ex);
		
		// remove the current connection the reconnect worker may recreate the connection later
		if (connection != null && connection.isConnected()) {
			connection.close();
		}
		connection = null;

	}

	/**
	 * Don't need to check for the backend to tune the channel ...
	 */
	public boolean tuneChannel(BackendListener listener, Channel channel) {
		listener.onMessage(name + " : " + "Channel tuned");
		return true;	
	}
	
	/**
	 * Calling the getTicket method of the HTSConnection for current channel.
	 * The call is synchronous and will wait 5 second to get the response from the server.
	 * @param channel
	 * @return
	 * @throws BackendException 
	 */
	public String getChannelUrl(BackendListener listener, Channel channel) throws BackendException {
		
		final HTSMessage message = new HTSMessage();		
		
		//final int channelID = channel.getID();
		message.setMethod("getTicket");
		message.putField("channelId", channel.getID());
		
		final StringBuffer responseMessage = new StringBuffer();
		
        HTSResponseHandler responseHandler = new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                
            	String path = response.getString("path", null);
                String ticket = response.getString("ticket", null);                
            	
                responseMessage.append(webroot);                
                responseMessage.append(path);
                responseMessage.append("/?ticket=");
                responseMessage.append(ticket);
                
                synchronized (message) {
                    message.notify();
                }
            }
        };

        connection.sendMessage(message, responseHandler);
        
        synchronized (message) {
            try {
                message.wait(500);
                return responseMessage.toString();
            } catch (InterruptedException ex) {
            	throw new BackendException("Failed to retrieve channel from backend");
            }
        }
	}
	
		
	/**
	 * Rely on connection in isConnected function
	 */
	public boolean isConnected() {
		return (connection != null && connection.isAuthenticated());
	}

	/**
	 * The default format for the input file that we get from TV Headend.
	 */
	public ConverterFormatEnum getDefaultFormat() {
		// TODO Auto-generated method stub
		return ConverterFormatEnum.MKV;
	}

	/**
	 * Getter for package classes.
	 * @return
	 */
	HTSConnection getConnection() {
		// TODO Auto-generated method stub
		return connection;
	}

	protected void getEPGDetails(final EPGEvent event) {
		
		final HTSMessage message = new HTSMessage();		
		
		logger.debug("Getting EPG Detail for event " + event.getEventId() + " from channel " + event.getChannel().name);
		message.setMethod("getEvent");		
		message.putField("eventId", event.getEventId());
		
		final StringBuffer responseMessage = new StringBuffer();
		
        HTSResponseHandler responseHandler = new HTSResponseHandler() {

            public void handleResponse(HTSMessage response) {
                
            	logger.debug("Got epg details for channel " + event.getChannel().name);
            	event.withStart(response.getLong("start",0))
            		 .withStop(response.getLong("stop",0))
            		 .withTitle(response.getString("title",""))
            		 .withDescription(response.getString("description",""))
            		 .withSummary(response.getString("summary",""));
            	 
            	event.getChannel().setEPGEvent(event);
            }
        };

        connection.sendMessage(message, responseHandler);
                
	}
	
	
	
}
