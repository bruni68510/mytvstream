package org.mytvstream.converter;

import com.xuggle.mediatool.*;
import com.xuggle.mediatool.event.IAddStreamEvent;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IStreamCoder;

public class TheoraConverter extends Converter {

	IMediaReader reader;
	IMediaWriter writer;
	
	static private class VideoSetting extends MediaToolAdapter {
		
		int videoBitrate;
		int audioBitrate;
		
		
		VideoSetting(int videoBitrate, int audioBbitrate) {
			super();
			
			this.videoBitrate = videoBitrate;
			this.audioBitrate = audioBitrate;
		}
		
		@Override
		public void onAddStream(IAddStreamEvent event) {
			
			int streamIndex = event.getStreamIndex();
			IStreamCoder streamCoder = event.getSource().getContainer().getStream(streamIndex).getStreamCoder();
			if (streamCoder.getCodecType() == streamCoder.getCodecType().CODEC_TYPE_AUDIO) {
				
				streamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, false); 
				streamCoder.setBitRate(audioBitrate);
				
			} 
			else if (streamCoder.getCodecType() == streamCoder.getCodecType().CODEC_TYPE_VIDEO) {
				
				streamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, false); 
				streamCoder.setBitRate(videoBitrate);
				
			}
			super.onAddStream(event);
			
		}
		
	}
	

	@Override
	public boolean openMedia(String mediaFile, ConverterFormatEnum inputFormat)
			throws ConverterException {
		
		reader = ToolFactory.makeReader(mediaFile);
		
		IContainerFormat format = IContainerFormat.make();
		format.setInputFormat(getConverterFormat(inputFormat));
		
		reader.getContainer().setFormat(format);
		
		return true;
	}

	@Override
	public boolean openOutput(String mediaFile, ConverterFormatEnum outputFormat)
			throws ConverterException {
		// TODO Auto-generated method stub
		
		writer = ToolFactory.makeWriter(mediaFile,reader);
		
		IContainerFormat format = IContainerFormat.make();
		format.setOutputFormat(getConverterFormat(outputFormat), null, null);
		
		writer.getContainer().setFormat(format);
		
		
		return true;
		
	}

	@Override
	public void setupReadStreams(String audioLanguage)
			throws ConverterException {
		// TODO Auto-generated method stub
		
		reader.open();
				
	}

	@Override
	public void setupWriteStreams(ConverterCodecEnum videoCodec,
			int videoBitrate, ConverterCodecEnum audioCodec, int audioBitrate)
			throws ConverterException {
		// TODO Auto-generated method stub
		
		writer.open();
		
		writer.addListener(new VideoSetting(videoBitrate, audioBitrate));
		
		reader.addListener(writer);
	}

	@Override
	protected void mainLoop() throws ConverterException {
		// TODO Auto-generated method stub
				
		do {
			reader.readPacket();
		} while (!closed);
	}

	@Override
	protected boolean CanHandle(String inputUrl,
			ConverterFormatEnum inputFormat, String outputUrl,
			ConverterFormatEnum outputFormat) {
		// TODO Auto-generated method stub
		return outputFormat.equals(outputFormat.OGG);
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
		if (format.equals(ConverterFormatEnum.WEBM))
			return "webm";
		return "";
	}
}
