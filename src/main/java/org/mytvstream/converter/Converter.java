package org.mytvstream.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter extends Thread implements IConverter {
	
	private final Logger logger = LoggerFactory.getLogger(Converter.class);
	
	protected boolean closed = false;
	
	public void close() {
		this.closed = true;
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
	protected void mainLoop() throws ConverterException
	{
		throw new ConverterException("main loop not implemented");
	}

	
	/**
	 * Determine if the converter can handle a particular conversion
	 * @param inputUrl : The input URL to process
	 * @param inputType : The input format type
	 * @param outputUrl : The output URL to process
	 * @param outputType : The output format type.
	 * @return
	 */
	protected boolean CanHandle(String inputUrl, ConverterFormatEnum inputFormat, String outputUrl, ConverterFormatEnum outputFormat) {
		
		logger.debug("calling can handle from Converter");
		return false;
	}
	
	
}
