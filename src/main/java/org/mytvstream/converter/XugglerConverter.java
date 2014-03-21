package org.mytvstream.converter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import com.xuggle.xuggler.ICodec.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.*;
import com.xuggle.xuggler.IAudioSamples.Format;

/**
 * Convert any input stream to flv for rtmp streaming.
 * @author cbrunner
 *
 */

public class XugglerConverter extends Converter {

	
	// input variables
	IContainer icontainer;
	IContainerFormat icontainerFormat;
	IPacket iPacket;
	IAudioSamples iAudioSamples;
	IVideoPicture iPicture;
	
	
	// input streams index
	int iAudioStreamIndex = -1;
	int iVideoStreamIndex = -1;
	
	// output variables
	IContainer ocontainer;
	IContainerFormat ocontainerFormat;
	IPacket oAudioPacket;
	IPacket oVideoPacket;
	IAudioSamples oTranscodedSamples;
	IAudioSamples oAudioSamples;
	int audioConsumed = 0;
	IVideoPicture oConvertedPicture;
	
	// output streams index
	int oAudioStreamIndex = -1;
	int oVideoStreamIndex = -1;
	
	// audio resampler
	IAudioResampler audioResampler;
	
	// video resampler
	IVideoResampler videoResampler;
	
	// static attributes
	static int VIDEO_WIDTH = 480;
	static int VIDEO_HEIGHT = 270;
	
	static final Logger logger = LoggerFactory.getLogger(XugglerConverter.class);
	
	/**
	 * Print informations about xuggler
	 */
	
	static {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		
		Configuration.printSupportedContainerFormats(ps);
		Configuration.printSupportedCodecs(ps);
		String output;
		try {
			output = os.toString("UTF8");
			logger.debug(output);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Open media let's set the url of the reading media
	 * @param mediaFile : url of the media files from the following formats (http, file) 
	 * @param inputFormat : Set the format of the media (avi, mov, flv, mkv, ts ...)
	 */
	public boolean openMedia(String mediaFile, ConverterFormatEnum inputFormat) throws ConverterException
	{
		icontainer = IContainer.make();
		icontainerFormat = IContainerFormat.make();
		int i = icontainerFormat.setInputFormat(getConverterFormat(inputFormat));
		
		
		if (i<0) {
			throw new ConverterException("failed to set input format " + inputFormat);
		}
		
		i = icontainer.open(mediaFile, IContainer.Type.READ, icontainerFormat);
		
		if (i < 0) {
			throw new ConverterException("could not open input media");
		}
		
		
		
		logger.debug(icontainer.toString());
		//System.out.println(icontainer.toString());
		return true;
	}
	
	/**
	 * openOutput : define the converter's output file
	 * @param mediaFile : url of the media file to be created
	 * @param outputFormat : Force the output format.
	 */
	
	public boolean openOutput(String mediaFile, ConverterFormatEnum outputFormat) throws ConverterException
	{
	
		ocontainer = IContainer.make();
		ocontainerFormat = IContainerFormat.make();
		ocontainerFormat.setOutputFormat(getConverterFormat(outputFormat), null, null);
		
		int i = ocontainer.open(mediaFile, IContainer.Type.WRITE, ocontainerFormat);
		
		if (i < 0 ) {
			throw new ConverterException("could not open output media");
		}
		
		logger.debug(ocontainer.toString());
		
		return true;
	}
	
	/**
	 * 	open converter output using an output stream
	 * @param stream : Output stream for writing 
	 * @param outputFormat : Output format to produce
	 * @return success / false
	 * @throws ConverterException
	 */
	public boolean openOutput(OutputStream stream, ConverterFormatEnum outputFormat) throws ConverterException {
		
		ocontainer = IContainer.make();
		ocontainerFormat = IContainerFormat.make();
		ocontainerFormat.setOutputFormat(getConverterFormat(outputFormat), null, null);
		
		//avoid crash with OutputStream
		ocontainer.setFormat(ocontainerFormat);
		
		int i = ocontainer.open(stream, ocontainerFormat);
		
		if (i < 0 ) {
			throw new ConverterException("could not open output media");
		}
		
		logger.debug(ocontainer.toString());
		
		return true;
	}
	/**
	 * Helper reading streams from 
	 * @param audioLanguage : Preferred audio language to use in the conversion (only one audio language will be transcoded).
	 */
	public void setupReadStreams(String audioLanguage)
	{
		for (int i = 0 ; i < icontainer.getNumStreams(); i++) {
			IStream stream = icontainer.getStream(i);
			IStreamCoder coder = stream.getStreamCoder();
			
			if (coder.getCodecType() == Type.CODEC_TYPE_VIDEO) {				
				iVideoStreamIndex = i;
			}
			if (coder.getCodecType() == Type.CODEC_TYPE_AUDIO) {
				if (iAudioStreamIndex == -1)
				{
					iAudioStreamIndex = i;
				}
				if (stream.getLanguage() != null && stream.getLanguage().equals(audioLanguage)) {
						iAudioStreamIndex = i;
				}				
			}			
		}
		
		logger.debug("Found video stream at " + iVideoStreamIndex);
		logger.debug("Found audio stream at " + iAudioStreamIndex);
		
	}
	
	/**
	 * Writes the streams to the output container created by openOutput call
	 * @param videoCodecName : Preferred video codec name, for FLV use either flv1 or libx264
	 * @param videoBitrate : Desired bitrate for the video
	 * @param audioCodecName : Preferred audio codec name, for FLV use either libmp3lame or libvo_aacenc
	 * @param audioBitrate : Desired bitrate for the audio (64000 may be ok).
	 * @throws Exception
	 */
	public void setupWriteStreams(ConverterCodecEnum videoEncoder, int videoBitrate, ConverterCodecEnum audioEncoder, int audioBitrate) throws ConverterException 
	{	
		// setup the output video stream
		
		ICodec videoCodec = ICodec.findEncodingCodecByName(getEncoderName(videoEncoder));
		if (videoCodec == null) {
			throw new ConverterException("could not find video encoder codec");
		}
		IStream videoStream = ocontainer.addNewStream(videoCodec);
		IStreamCoder videoCoder = videoStream.getStreamCoder();
		
		videoCoder.setBitRate(videoBitrate);
		
		// fps 25
		videoCoder.setTimeBase(IRational.make(1, 25));
				
		videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
		
		// height and width from input container
		videoCoder.setHeight(VIDEO_HEIGHT);
		videoCoder.setWidth(VIDEO_WIDTH);
		
		logger.debug("video height = " + videoCoder.getHeight() + ", width =" + videoCoder.getWidth());

		oVideoStreamIndex = videoStream.getIndex();			
		
		// setup the output audio stream
		ICodec audioCodec = ICodec.findEncodingCodecByName(getEncoderName(audioEncoder));
		if (audioCodec == null) 
		{
			throw new ConverterException("could not find audio encoder codec");
		}
		
		IStream audioStream = ocontainer.addNewStream(audioCodec);
		IStreamCoder audioCoder = audioStream.getStreamCoder();
		
		audioCoder.setSampleRate(44100);
		audioCoder.setTimeBase(IRational.make(1, 44100));
		audioCoder.setChannels(2);
		audioCoder.setBitRate(audioBitrate);
		
		if (audioEncoder == ConverterCodecEnum.VORBIS) {
			audioCoder.setSampleFormat(Format.FMT_FLT);
		}
		else {
			audioCoder.setSampleFormat(Format.FMT_S16);
		}
		
		oAudioStreamIndex = audioStream.getIndex();
		
		// once finished write header
		
	}
	
	
	protected boolean CanHandle(String inputUrl, ConverterFormatEnum inputFormat, String outputUrl, ConverterFormatEnum outputFormat)
	{
		logger.debug("Calling canHandle from xugglerconverter");
		
		return true;
	}
	
	/**
	 * Process a video packet:
	 * 	- Decode the video packet from input container
	 *  - Encode the video packet into output container
	 *  - Write the packet to output stream
	 * @throws Exception
	 */
	protected void processVideo() throws ConverterException
	{
	
		int rv = icontainer.getStream(iVideoStreamIndex).getStreamCoder().decodeVideo(iPicture, iPacket, 0);
	      if (rv < 0)
	        throw new ConverterException("error decoding video " + rv);

	      // if this is a complete picture, dispatch the picture

	      if (iPicture.isComplete()) {
	    	  
	    	  videoResampler.resample(oConvertedPicture, iPicture);
	    	  
	    	  if (oConvertedPicture.isComplete()) {
	    		  if (ocontainer.getStream(oVideoStreamIndex).getStreamCoder().encodeVideo(oVideoPacket, oConvertedPicture, 0) < 0)
	    			  throw new ConverterException("failed to encode video");
	    	  
	    		  if (oVideoPacket.isComplete()) {
	    			  int result = ocontainer.writePacket(oVideoPacket, true);
	    			  	    			  
	            	  if (result < 0 ) {
	            		  throw new ConverterException("Failed to write video to output");
	            	  }
	            	  
	    			  oVideoPacket.delete();
	    			  oVideoPacket = IPacket.make();
	    		  }
	    	  }
	      }
	    	  
	}
	
	/**
	 * Process a audio packet:
	 * 	- Decode the audio packet from input container
	 *  - Encode the audio packet into output container
	 *  - Write the packet to output stream
	 * @throws Exception
	 */
	protected void processAudio() throws ConverterException
	{
	    // packet may contain multiple audio frames, decode audio until
	    // all audio frames are extracted from the packet 
	    
	    int offset = 0;
	    while (offset < iPacket.getSize())
	    {
	      
	    	// decode audio	      
	    	int bytesDecoded = icontainer.getStream(iAudioStreamIndex).getStreamCoder().decodeAudio(iAudioSamples, iPacket, offset);
	    	if (bytesDecoded < 0)
	    		throw new ConverterException("error " + bytesDecoded + " decoding audio");
	    	
	    	offset += bytesDecoded;
	        	
        	audioResampler.resample(oTranscodedSamples, iAudioSamples, iAudioSamples.getNumSamples());
        	
        	//oTranscodedSamples.setTimeStamp(iAudioSamples.getTimeStamp());
        	
        	
        	for (;audioConsumed < oTranscodedSamples.getNumSamples(); /* in loop */)
    	    {
    	        // encode audio
              int result = ocontainer.getStream(oAudioStreamIndex).getStreamCoder().encodeAudio(oAudioPacket, oTranscodedSamples, audioConsumed); 
              if (result < 0)
                throw new ConverterException("failed to encode audio");

              // update total consumed

              audioConsumed += result;
              // if a complete packed was produced write it out
    	      
              if (oAudioPacket.isComplete()) {
            	  logger.trace("writing audio packet");
            	  result = ocontainer.writePacket(oAudioPacket, true);
            	  if (result < 0 ) {
            		  throw new ConverterException("Failed to write to output");
            	  }
            	  //oAudioPacket.delete();
            	  //oAudioPacket = IPacket.make();
              }

    	    } 
    	    
    	    if (audioConsumed >= oTranscodedSamples.getNumSamples()) {	    	    	
    	    	audioConsumed = 0;
    	    }
    	
	    }
	}
	
	/**
	 * Main processing loop, reading packet from input container and processing that packet.
	 * @throws Exception
	 */
	 @Override
	protected void mainLoop() throws ConverterException 
	{
		
		final String orgName = Thread.currentThread().getName();
	    Thread.currentThread().setName(orgName + " - Xuggler Thread");
		 
		closed = false;
		
		iPacket = IPacket.make();
		oAudioPacket = IPacket.make();
		oVideoPacket = IPacket.make();
		iAudioSamples = IAudioSamples.make(4096, 2);		
		oTranscodedSamples = IAudioSamples.make(4096,2);
		iPicture = IVideoPicture.make(icontainer.getStream(iVideoStreamIndex).getStreamCoder().getPixelType(), 
			icontainer.getStream(iVideoStreamIndex).getStreamCoder().getWidth(), 
			icontainer.getStream(iVideoStreamIndex).getStreamCoder().getHeight()
		);
	
		oConvertedPicture = IVideoPicture.make(ocontainer.getStream(oVideoStreamIndex).getStreamCoder().getPixelType(), 
			ocontainer.getStream(oVideoStreamIndex).getStreamCoder().getWidth(), 
			ocontainer.getStream(oVideoStreamIndex).getStreamCoder().getHeight()
		); 
		
		icontainer.getStream(iAudioStreamIndex).getStreamCoder().open(null,null);
		ocontainer.getStream(oAudioStreamIndex).getStreamCoder().open(null,null);
		icontainer.getStream(iVideoStreamIndex).getStreamCoder().open(null,null);
		ocontainer.getStream(oVideoStreamIndex).getStreamCoder().open(null,null);
		
		audioResampler = IAudioResampler.make(2, 2, 44100, icontainer.getStream(iAudioStreamIndex).getStreamCoder().getSampleRate());
		if (audioResampler == null) 
		{
			throw new ConverterException("can't setup audio resampler");
		}

		videoResampler = IVideoResampler.make(VIDEO_WIDTH, VIDEO_HEIGHT, IPixelFormat.Type.YUV420P, 
			icontainer.getStream(iVideoStreamIndex).getStreamCoder().getWidth(), 
			icontainer.getStream(iVideoStreamIndex).getStreamCoder().getHeight(),
			icontainer.getStream(iVideoStreamIndex).getStreamCoder().getPixelType()
		);
		
		if (videoResampler == null) 
		{
			throw new ConverterException("can't setup video resampler");
		}
		
		ocontainer.writeHeader();
	
		
		while (icontainer.readNextPacket(iPacket) >= 0 && !closed) {
			if (iPacket.getStreamIndex() == iAudioStreamIndex)
			{							
				processAudio();	
			}
			if (iPacket.getStreamIndex() == iVideoStreamIndex)
			{				
				processVideo();	
			}
		}
		
		iPacket.delete();
		oAudioPacket.delete();
		oVideoPacket.delete();
		iAudioSamples.delete();
		oTranscodedSamples.delete();
		iPicture.delete();
		oConvertedPicture.delete();
		ocontainer.writeTrailer();
		ocontainer.getStream(oAudioStreamIndex).getStreamCoder().close();
		ocontainer.getStream(oVideoStreamIndex).getStreamCoder().close();		
		ocontainer.close();
		
		icontainer.getStream(iAudioStreamIndex).getStreamCoder().close();
		icontainer.getStream(iVideoStreamIndex).getStreamCoder().close();
		icontainer.close();
	}
	
	protected String getEncoderName(ConverterCodecEnum codec) {
		
		if (codec.equals(ConverterCodecEnum.H264)) {
			return "libx264";
		}
		if (codec.equals(ConverterCodecEnum.AAC)) {
			//return "libvo_aacenc";
			return "aac";
		}
		if (codec.equals(ConverterCodecEnum.MP3)) {
			return "libmp3lame";
		}
		if (codec.equals(ConverterCodecEnum.FLV1)) {
			return "flv";
		}
		if (codec.equals(ConverterCodecEnum.VORBIS)) {
			return "libvorbis";
		}
		if (codec.equals(ConverterCodecEnum.THEORA)) {
			return "libtheora";
		}
		return null;
	}
		
	
	protected String getConverterFormat(ConverterFormatEnum format) {
		if (format.equals(ConverterFormatEnum.FLV))
			return "flv";
		if (format.equals(ConverterFormatEnum.OGG))
			return "ogg";
		if (format.equals(ConverterFormatEnum.MKV))
			return "matroska";
		if (format.equals(ConverterFormatEnum.HLS))
			return "applehttp";
		return "";
	}
	
	
}
