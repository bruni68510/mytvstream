package org.mytvstream.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tvheadend.tvhguide.htsp.HTSConnection;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSMessage;
import org.tvheadend.tvhguide.htsp.HTSResponseHandler;

/**
 * Internal storage of an hts channel.
 * @author cbrunner
 *
 */
public class Channel implements Comparable<Channel> {
	
	/**
	 * Attributes
	 */
	protected String name;
	protected int id;
	protected int number;
	protected String serviceID;
	protected String currentURL;
	
	// Contains the current EPG Event.
	protected EPGEvent event;
	
	private Logger logger = LoggerFactory.getLogger(Channel.class);
	
	/**
	 * 
	 * @param channelName : Channel name for this channel
	 * @param channelID : Channel ID for this channel
	 * @param channelNumber : Channel number for this channel.
	 */
	public Channel(String name, int id, int number) 
	{
		this.name = name;
		this.id = id;
		this.number = number;		
		this.event = null;
	}
	
	
	
	/**
	 * Setters
	 */
	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	
	public void setEPGEvent(EPGEvent event) {
		this.event = event;
	}
	
	/**
	 * Getters
	 */
	
	public int getID()
	{
		return id;
	}
	
	public String getName() 
	{
		return name;
	}
	
	public int getNumber() {
		return number;
	}


	public EPGEvent getEPGEvent() {
		return event;
	}
	
	public int compareTo(Channel o) {
		// TODO Auto-generated method stub
		return new Integer(number).compareTo(o.number);	
	}
	
	public boolean hasEPGEvent() {
		return event != null;
	}
}
