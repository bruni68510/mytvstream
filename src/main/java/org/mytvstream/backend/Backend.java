package org.mytvstream.backend;

import java.util.ArrayList;

import org.tvheadend.tvhguide.htsp.HTSMessage;
import org.tvheadend.tvhguide.htsp.HTSResponseHandler;

public abstract class Backend implements IBackend {
	
	// The name of the backend
	protected String name = "Default backend";
	
	protected int id = 0; 
	
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
	 * Calling the getTicket method of the HTSConnection for current channel.
	 * @param connection
	 * @return
	 */
	public String getChannelUrl(int channelID) {		
		return "";
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
}
