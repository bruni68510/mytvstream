package org.mytvstream.producerserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.mytvstream.converter.Converter;
import org.mytvstream.converter.ConverterException;
import org.mytvstream.converter.ConverterFactory;
import org.mytvstream.converter.ConverterFormatEnum;
import org.mytvstream.converter.XugglerConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The producer factory is responsible to find a suitable producer for a given stream
 * 
 * @author cbrunner
 *
 */
public class ProducerServerFactory {

	/**
	 * Singleton definition
	 */
	private ProducerServerFactory(){}
	private static ProducerServerFactory SINGLETON = new ProducerServerFactory();
	public static ProducerServerFactory getInstance()
	{	
		return SINGLETON;
	}
	
	
	/**
	 * Logger definition
	 */
	private static Logger logger = LoggerFactory.getLogger(ProducerServerFactory.class);
	
	/**
	 * Factory implementation
	 */
	// registering converters
	static HashMap<String, Class<?>>registeredProducerServer = new HashMap<String, Class<?>>(); 
	
	static {
		registeredProducerServer.put("RTMPProducerServer", RTMPProducerServer.class);		
	}
	
	/**
	 * Factory method
	 * @param url : url to produce
	 */
	public ProducerServer getProducerServer(
		String url
	) throws ProducerServerException {

	    
		logger.debug("getting producer server for " + url);
		
		ProducerServer producerServer = null;
		
		Iterator<Entry<String, Class<?>>> it = registeredProducerServer.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Class<?>> pairs = it.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        
	        Class<?> producerServerClass = (Class<?>) pairs.getValue();	        
			try {
				producerServer = (ProducerServer)producerServerClass.newInstance();
				if (producerServer.canServe(url))  {
		        	return producerServer;
		        }
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				throw new ProducerServerException("Error getting producer server : " + e.getMessage());
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				throw new ProducerServerException("Error getting producer server: " + e.getMessage());
			} 	       
	    }
 
	    
	    throw new ProducerServerException("Can't find suitable producer server");
	}
}
