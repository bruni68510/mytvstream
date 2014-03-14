package org.mytvstream.producerserver;

abstract public class ProducerServer extends Thread {

	protected String url;
	
	public boolean canServe(String url) {
		return false;
	}
	
	public void setOutputUrl(String url) {
		this.url = url;
	}
	
}
