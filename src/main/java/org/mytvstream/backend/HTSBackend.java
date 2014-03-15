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
	private HTSConnection connection;
	private String webroot = new String();
	private String username;
	private String password;
	private String server;
	private short port;
	private short httpport;
	
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
	
		this.id = id;
		
		bouquets.add(new Bouquet(0,bouquetName,0));
		
		server = backendConfiguration.getServer();
		port = backendConfiguration.getPort();
		username = backendConfiguration.getUsernmame();
		password = backendConfiguration.getPassword();
		httpport = backendConfiguration.getHttpport();
		name = "HTS Backend " + backendConfiguration.getServer();
	}

	public void connect() throws BackendException {
		
		connection = new HTSConnection(this, HTSBackend.class.getPackage().getName(), HTSBackend.class.getName());		
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
	
	/**
	 * The HTS backend has delivered an asynchronous message
	 * See HTSP definition at https://tvheadend.org/projects/tvheadend/wiki/Htsp for the list of possible messages
	 */
	public void onMessage(HTSMessage response) {
		if (response.getMethod().equals("channelAdd")) {
			//logger.debug("Adding channel " + response.);
			
			long channelID = response.getLong("channelId");
	        String ChannelName = response.getString("channelName", null);
	        int channelNumber = response.getInt("channelNumber", 0);
	        
	        if (channelNumber == 0) {
	        	channelNumber = 1000;
	        }
	        
	        Channel newChannel = new Channel(ChannelName, (int)channelID, channelNumber);
	        //channels.add(newChannel);
	        bouquets.get(0).channels.add(newChannel);
	        Collections.sort(bouquets.get(0).channels);
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
	 * Calling the getTicket method of the HTSConnection for current channel.
	 * The call is synchronous and will wait 5 second to get the response from the server.
	 * @param channel
	 * @return
	 */
	public String getChannelUrl(final int channelID) {
		
		final HTSMessage message = new HTSMessage();
		//final int channelID = channel.getID();
		message.setMethod("getTicket");
		message.putField("channelId", channelID);
		
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
                message.wait(5000);
                return responseMessage.toString();
            } catch (InterruptedException ex) {
            	return "";
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
}
