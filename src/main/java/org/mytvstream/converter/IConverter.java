package org.mytvstream.converter;

import java.io.OutputStream;

public interface IConverter {

	/**
	 * Open media let's set the url of the reading media
	 * @param mediaFile : url of the media files from the following formats (http, file) 
	 * @param inputFormat : Set the format of the media (avi, mov, flv, mkv, ts ...)
	 */
	public boolean openMedia(String mediaFile, ConverterFormatEnum inputFormat) throws ConverterException;
	
	/**
	 * openOutput : define the converter's output file
	 * @param mediaFile : url of the media file to be created
	 * @param outputFormat : Force the output format.
	 */
	public boolean openOutput(String mediaFile, ConverterFormatEnum outputFormat) throws ConverterException;
	public boolean openOutput(OutputStream stream, ConverterFormatEnum outputFormat) throws ConverterException;
	
	public void setupReadStreams(String audioLanguage) throws ConverterException;
	
	public void setupWriteStreams(ConverterCodecEnum videoCodec, int videoBitrate, ConverterCodecEnum audioCodec, int audioBitrate) throws ConverterException;
	
	public void close();
	
}