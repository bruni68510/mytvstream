package org.mytvstream.backend;

import java.util.ArrayList;

public class Bouquet implements Comparable<Bouquet> {

	protected String name;
	protected int id;
	protected int number;
	
	/**
	 * List of channels for this backend
	 */
	protected ArrayList<Channel> channels = new ArrayList<Channel>();
	
	public Bouquet(int id, String name, int number) {
		this.name = name;
		this.id = id;
		this.number = number;
	}
	
	public String getName() {
		return name;
	}
	
	public int getID() {
		return id;
	}
	
	public int getNumber() {
		return number;
	}
	
	public ArrayList<Channel> getChannels() {
		return channels;		
	}

	public int compareTo(Bouquet arg0) {
		// TODO Auto-generated method stub
		return new Integer(number).compareTo(arg0.number);
	}
	
}
