package org.mytvstream.backend;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class EPGEvent {

	private long eventId;
	private Channel channel;
	private long start = 0;
	private long stop = 0;
	
	private String title = "";
	private String summary = "";
	private String description = "";
	
	private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm";
	
	/**
	 * Minimal constructor with eventID and channelID
	 * @param eventId
	 * @param channelId
	 */
	public EPGEvent(long eventId, Channel channel) {
		this.eventId = eventId;
		this.channel = channel;
	}
	
	/**
	 * Getters and setters, builder style
	 */
	public EPGEvent withStart(long start) {
		this.start = start;
		return this;
	}
	
	public EPGEvent withStop(long stop) {
		this.stop = stop;
		return this;
	}
	
	public EPGEvent withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public EPGEvent withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public EPGEvent withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public long getEventId() {
		return eventId;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	
	public String getStart() {
		return getFormattedDate(start);
	}
	
	public String getStop() {
		return getFormattedDate(stop);
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSummary() {
		return summary;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String toString() {
		//return title + " From " + getStart() + " To " + getStop() + "\n" + summary + "\n" + description;
		
		StringBuilder result = new StringBuilder();
		result.append(title);
		result.append(":From " + getStart() + " To " + getStop());
		result.append("\n");
		result.append(summary);
		result.append("\n");
		result.append(description);
		return result.toString();
	}
	
	/**
	 * Helpers
	 */
	protected String getFormattedDate(long timestamp) {
		Date date = new Date(timestamp*1000L); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT); // the format of your date
		sdf.setTimeZone(TimeZone.getDefault());
		return sdf.format(date);
	}
}
