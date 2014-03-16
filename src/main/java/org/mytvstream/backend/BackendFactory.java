package org.mytvstream.backend;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import org.mytvstream.converter.ConverterException;
import org.mytvstream.producerserver.ProducerServer;
import org.mytvstream.producerserver.ProducerServerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The backend factory is responsible to find the suitable
 * backend for a given type.
 * Supported types are : htsp.
 * @author cbrunner
 *
 */
public class BackendFactory {

	/**
	 * Singleton definition
	 */
	private static BackendFactory SINGLETON = new BackendFactory();
	private BackendFactory() {}
	public static BackendFactory getInstance() {
		return SINGLETON;
	}
		
	/**
	 * Logger definition
	 */
	private static Logger logger = LoggerFactory.getLogger(Backend.class);
	
	/**
	 * Factory method
	 * @param url : url to produce
	 */
	public Backend getBackend(
		int id,
		String type,
		org.mytvstream.configuration.Configuration.Backends.Backend backendConfiguration
	) throws BackendException {

	    
		logger.debug("getting backend for " + type);
		
		if (type.equals(BackendType.HTS.type)) {
			return new HTSBackend(id,backendConfiguration);
		}
		if (type.equals(BackendType.EYETV.type)) {
			return new EyeTvBackend(id,backendConfiguration);
		}
	    
	    throw new BackendException("Can't find suitable backend");
	}
}
