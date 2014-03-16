package org.mytvstream.backend;

import java.util.ArrayList;
import java.util.Hashtable;

import org.mytvstream.converter.ConverterFormatEnum;

public interface IBackend {

	public ArrayList<Bouquet> getBouquets();
	
	public ArrayList<Channel> getChannels(Bouquet bouquet);
	
	public String getChannelUrl(int channelID) throws BackendException;
	
	public boolean tuneChannel(BackendListener listener, String channel) throws BackendException;
	
	public ConverterFormatEnum getDefaultFormat();
	
	public void connect() throws BackendException;
	
	public boolean isConnected();
	
	
}
