package org.mytvstream.backend;

import java.util.ArrayList;

import org.tvheadend.tvhguide.htsp.HTSConnection;
import org.tvheadend.tvhguide.htsp.HTSMessage;
import org.tvheadend.tvhguide.htsp.HTSResponseHandler;

public abstract class Backend implements IBackend {
	
	// The name of the backend
	protected String name = "Default backend";
	
	// the id of the backend
	protected int id = 0; 
	
	// is the backend connected
	protected boolean connected = false;
	
	/**
	 * Backend connection
	 */	
	protected String username;
	protected String password;
	protected String server;
	protected short port;
	protected short httpport;
	
	/**
	 * List of channels for this backend
	 */
	protected ArrayList<Bouquet> bouquets = new ArrayList<Bouquet>();
	
	/**
	 * Get a list of channels.
	 */
	public ArrayList<Bouquet> getBouquets() {
		// TODO Auto-generated method stub
		return bouquets;
	}
	
	public ArrayList<Channel> getChannels(Bouquet bouquet) {
		return bouquet.getChannels();
	}
	
	/**
	 * Not implemented on generic backend
	 * @param connection
	 * @return
	 */
	public String getChannelUrl(int channelID) throws BackendException {		
		throw new BackendException("Not available");
	}
	
	/**
	 * Not implemented on generic backend
	 * 
	 */
	public boolean tuneChannel(String channelURL) throws BackendException {
		throw new BackendException("Not available");
	}
		
	/**
	 * Get the registered name of the backend
	 * The name is typically set be child classes
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the ID of the backend
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Is connected
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Default constructor of backend, stores configuration into local variables.
	 * @param id
	 * @param backendConfiguration
	 */
	public Backend(int id, org.mytvstream.configuration.Configuration.Backends.Backend backendConfiguration) {
		this.id = id;
		server = backendConfiguration.getServer();
		port = backendConfiguration.getPort();
		username = backendConfiguration.getUsernmame();
		password = backendConfiguration.getPassword();
		httpport = backendConfiguration.getHttpport();
	}
}
