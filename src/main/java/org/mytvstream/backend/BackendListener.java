package org.mytvstream.backend;

/**
 * The backend listener is used by the backend while performing tasks
 * to notify the caller about status of processing
 * @author cbrunner
 *
 */
public interface BackendListener {

	public void onMessage(String message);	
	
}
