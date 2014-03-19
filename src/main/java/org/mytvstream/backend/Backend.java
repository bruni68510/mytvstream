package org.mytvstream.backend;

import java.util.ArrayList;
import java.util.Iterator;

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
	 * Simple getters.
	 */
	public ArrayList<Bouquet> getBouquets() {
		return bouquets;
	}
	
	public ArrayList<Channel> getChannels(Bouquet bouquet) {
		return bouquet.getChannels();
	}
	
	
	
	/**
	 * Retrieve a bouquet from it's ID
	 */
	public Bouquet getBouquetByID(int bouquetID) throws BackendException {
		// TODO Auto-generated method stub
		Bouquet bouquet = null;
		
		Iterator<Bouquet> it = bouquets.iterator();
		
		while (it.hasNext()) {
			bouquet = it.next();
			if (bouquet.getID() == bouquetID) {
				return bouquet;
			}
				
		}
		
		throw new BackendException("bouquet not found");
		
	}
	/**
	 * Get a channel from it's bouquet and channel ID
	 * @param bouquet
	 * @param channelID
	 * @return
	 * @throws BackendException
	 */
	public Channel getChannelByID(Bouquet bouquet, int channelID) throws BackendException {
		Channel channel = null;
		
		
		Iterator<Channel> it = bouquet.channels.iterator();
		
		while(it.hasNext()) {
			channel = it.next();
			if (channel.getID() == channelID) {
				return channel;
			}
		}
		
		throw new BackendException("Channel not found");
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
	
	/**
	 * Copy constructor
	 * @param backend
	 */
	protected Backend() {	}
}
