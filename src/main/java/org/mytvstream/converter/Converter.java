package org.mytvstream.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter extends Thread implements IConverter {
	
	private final Logger logger = LoggerFactory.getLogger(Converter.class);
	
	protected boolean closed = false;
	
	public void close() {
				
		if (isAlive()) {
		
			this.closed = true;
			
			logger.debug("Closing current converter");
						
			try {
				join(2000);
			} catch (InterruptedException e) {
				logger.error("Timeout error waiting for converter to close :" + e.getMessage());
			}			
			
		}
		
		
	}
	
	public void run() {
		try {
			mainLoop();
		} catch (ConverterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * The main loop for the conversion, not implemented in converter.
	 * @throws ConverterException
	 */
	abstract protected void mainLoop() throws ConverterException;
	
	
	/**
	 * Determine if the converter can handle a particular conversion
	 * @param inputUrl : The input URL to process
	 * @param inputType : The input format type
	 * @param outputUrl : The output URL to process
	 * @param outputType : The output format type.
	 * @return
	 */
	abstract protected boolean CanHandle(String inputUrl, ConverterFormatEnum inputFormat, String outputUrl, ConverterFormatEnum outputFormat);
	
}
