package org.mytvstream.backend;

import java.util.ArrayList;
import java.util.Hashtable;

import org.mytvstream.converter.ConverterFormatEnum;

public interface IBackend {

	public ArrayList<Bouquet> getBouquets();
	
	public ArrayList<Channel> getChannels(Bouquet bouquet);
	
	public String getChannelUrl(int channelID);
	
	public ConverterFormatEnum getDefaultFormat();
	
	
}
