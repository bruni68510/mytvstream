package org.mytvstream.backend;

import java.util.ArrayList;
import java.util.Hashtable;

import org.mytvstream.converter.ConverterFormatEnum;

public interface IBackend {
	
	public String getChannelUrl(BackendListener listener, Channel channel) throws BackendException;
	
	public boolean tuneChannel(BackendListener listener, Channel channel) throws BackendException;
	
	public ConverterFormatEnum getDefaultFormat();
	
	public void connect() throws BackendException;
	
	public boolean isConnected();
	
	
}
