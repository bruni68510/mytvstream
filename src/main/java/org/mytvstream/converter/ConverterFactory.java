package org.mytvstream.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterFactory {

	/**
	 * Logger definition
	 */
	private static final Logger logger = LoggerFactory.getLogger(ConverterFactory.class);
	
	
	/**
	 * Singleton definition
	 */
	private ConverterFactory(){}

	private static ConverterFactory INSTANCE = new ConverterFactory();
 
	public static ConverterFactory getInstance()
	{	
		return INSTANCE;
	}
			
	// registering converters
	static HashMap<String, Class<?>>registeredConverter = new HashMap<String, Class<?>>(); 
	
	static {
		registeredConverter.put("XugglerConverter", XugglerConverter.class);		
	}
	
	
	/**
	 * Factory method
	 * @param inputUrl
	 * @param inputFormat
	 * @param outputUrl
	 * @param outputFormat
	 * @return
	 * @throws ConverterException
	 */
	public Converter getConverter(
		String inputUrl,
		ConverterFormatEnum inputFormat,
		String outputUrl,
		ConverterFormatEnum outputFormat
	) throws ConverterException {

	    
		logger.debug("getting converter for " + inputUrl);
		
		Converter converter = null;
		
		Iterator<Entry<String, Class<?>>> it = registeredConverter.entrySet().iterator();
	    while (it.hasNext()) {
	        Entry<String, Class<?>> pairs = it.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        
	        Class<?> converterClass = (Class<?>) pairs.getValue();	        
			try {
				converter = (Converter)converterClass.newInstance();
				if (converter.CanHandle(inputUrl, inputFormat, outputUrl, outputFormat))  {
					//converter.openMedia(inputUrl, inputFormat);
					//converter.openOutput(outputUrl, outputFormat);
		        	return converter;
		        }
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				throw new ConverterException("Error getting converter : " + e.getMessage());
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				throw new ConverterException("Error getting converter: " + e.getMessage());
			} 	       
	    }
 
	    
	    throw new ConverterException("Can't find suitable converter");
	}
		
		
}
