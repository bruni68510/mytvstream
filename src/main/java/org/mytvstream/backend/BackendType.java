package org.mytvstream.backend;

public enum BackendType {
	HTS("htsp"),
	EYETV("eyetv");
	
	protected String type;
	
	BackendType(String type) {
		this.type = type;
	}
}
