package org.mytvstream.backend;

public enum BackendType {
	HTS("htsp");
	
	protected String type;
	
	BackendType(String type) {
		this.type = type;
	}
}
