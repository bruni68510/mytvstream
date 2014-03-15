package org.mytvstream.backend;

import java.util.Iterator;

import org.mytvstream.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendReconnectThread extends Thread {
	
	/**
	 * private logger
	 */
	private Logger logger = LoggerFactory.getLogger(BackendReconnectThread.class);
	
	public boolean interrupted = false;
	
	private static int THREAD_WAIT = 5000;
	
	public void run() {
		
		while (!interrupted) {
		
			Iterator<Backend> it = Main.getInstance().getBackend().iterator();
			while (it.hasNext()) {
				Backend backend = it.next();
				
				if (!backend.isConnected()) {
					try {
						backend.connect();
					} catch (BackendException e) {
						// TODO Auto-generated catch block
						logger.error("Can't connect to backend " + backend.getName() + " for the moment");
					}
				}
			}
			
			try {
				Thread.sleep(THREAD_WAIT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.error("Connect thread interrupted");
			}
		}
		
	}
	

}
