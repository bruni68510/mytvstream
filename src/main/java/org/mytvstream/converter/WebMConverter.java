package org.mytvstream.converter;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IMetaData;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.URLProtocolManager;

public class WebMConverter extends Converter {

	static
	{
		// this forces the FFMPEG io library to be loaded which means we can bypass
		// FFMPEG's file io if needed
		URLProtocolManager.getManager();
	}

	/**
	 * Create a new Converter object.
	 */
	public WebMConverter()
	{

	}

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * A container we'll use to read data from.
	 */
	private IContainer mIContainer = null;
	/**
	 * A container we'll use to write data from.
	 */
	private IContainer mOContainer = null;

	/**
	 * A set of {@link IStream} values for each stream in the input
	 * {@link IContainer}.
	 */
	private IStream[] mIStreams = null;
	/**
	 * A set of {@link IStreamCoder} objects we'll use to decode audio and video.
	 */
	private IStreamCoder[] mICoders = null;

	/**
	 * A set of {@link IStream} objects for each stream we'll output to the output
	 * {@link IContainer}.
	 */
	private IStream[] mOStreams = null;
	/**
	 * A set of {@link IStreamCoder} objects we'll use to encode audio and video.
	 */
	private IStreamCoder[] mOCoders = null;

	/**
	 * A set of {@link IVideoPicture} objects that we'll use to hold decoded video
	 * data.
	 */
	private IVideoPicture[] mIVideoPictures = null;
	/**
	 * A set of {@link IVideoPicture} objects we'll use to hold
	 * potentially-resampled video data before we encode it.
	 */
	private IVideoPicture[] mOVideoPictures = null;

	/**
	 * A set of {@link IAudioSamples} objects we'll use to hold decoded audio
	 * data.
	 */
	private IAudioSamples[] mISamples = null;
	/**
	 * A set of {@link IAudioSamples} objects we'll use to hold
	 * potentially-resampled audio data before we encode it.
	 */
	private IAudioSamples[] mOSamples = null;

	/**
	 * A set of {@link IAudioResampler} objects (one for each stream) we'll use to
	 * resample audio if needed.
	 */
	private IAudioResampler[] mASamplers = null;
	/**
	 * A set of {@link IVideoResampler} objects (one for each stream) we'll use to
	 * resample video if needed.
	 */
	private IVideoResampler[] mVSamplers = null;

	/**
	 * Should we convert audio
	 */
	private boolean mHasAudio = true;
	/**
	 * Should we convert video
	 */
	private boolean mHasVideo = true;

	/**
	 * Should we force an interleaving of the output
	 */
	private final boolean mForceInterleave = true;

	/**
	 * Should we attempt to encode 'in real time'
	 */
	private boolean mRealTimeEncoder;

	private Long mStartClockTime;
	private Long mStartStreamTime;
	private IContainerFormat oFmt;
	
	/**
	 * Global variables
	 */
	String outputURL;
	String inputURL;
	
	

	/**
	 * Close and release all resources we used to run this program.
	 */
	void closeStreams() throws ConverterException
	{
		int numStreams = 0;
		int i = 0;

		numStreams = mIContainer.getNumStreams();
		/**
		 * Some video coders (e.g. MP3) will often "read-ahead" in a stream and keep
		 * extra data around to get efficient compression. But they need some way to
		 * know they're never going to get more data. The convention for that case
		 * is to pass null for the IMediaData (e.g. IAudioSamples or IVideoPicture)
		 * in encodeAudio(...) or encodeVideo(...) once before closing the coder.
		 * 
		 * In that case, the IStreamCoder will flush all data.
		 */
		for (i = 0; i < numStreams; i++)
		{
			if (mOCoders[i] != null)
			{
				IPacket oPacket = IPacket.make();
				do {
					if (mOCoders[i].getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
						mOCoders[i].encodeAudio(oPacket, null, 0);
					else
						mOCoders[i].encodeVideo(oPacket, null, 0);
					if (oPacket.isComplete())
						mOContainer.writePacket(oPacket, mForceInterleave);
				} while (oPacket.isComplete());
			}
		}
		/**
		 * Some container formats require a trailer to be written to avoid a corrupt
		 * files.
		 * 
		 * Others, such as the FLV container muxer, will take a writeTrailer() call
		 * to tell it to seek() back to the start of the output file and write the
		 * (now known) duration into the Meta Data.
		 * 
		 * So trailers are required. In general if a format is a streaming format,
		 * then the writeTrailer() will never seek backwards.
		 * 
		 * Make sure you don't close your codecs before you write your trailer, or
		 * we'll complain loudly and not actually write a trailer.
		 */
		int retval = mOContainer.writeTrailer();
		if (retval < 0)
			throw new ConverterException("Could not write trailer to output file");

		/**
		 * We do a nice clean-up here to show you how you should do it.
		 * 
		 * That said, Xuggler goes to great pains to clean up after you if you
		 * forget to release things. But still, you should be a good boy or giral
		 * and clean up yourself.
		 */
		for (i = 0; i < numStreams; i++)
		{
			if (mOCoders[i] != null)
			{
				/**
				 * And close the input coder to tell Xuggler it can release all native
				 * memory.
				 */
				mOCoders[i].close();
			}
			mOCoders[i] = null;
			if (mICoders[i] != null)
				/**
				 * Close the input coder to tell Xuggler it can release all native
				 * memory.
				 */
				mICoders[i].close();
			mICoders[i] = null;
		}

		/**
		 * Tell Xuggler it can close the output file, write all data, and free all
		 * relevant memory.
		 */
		mOContainer.close();
		/**
		 * And do the same with the input file.
		 */
		mIContainer.close();

		/**
		 * Technically setting everything to null here doesn't do anything but tell
		 * Java it can collect the memory it used.
		 * 
		 * The interesting thing to note here is that if you forget to close() a
		 * Xuggler object, but also loose all references to it from Java, you won't
		 * leak the native memory. Instead, we'll clean up after you, but we'll
		 * complain LOUDLY in your logs, so you really don't want to do that.
		 */
		mOContainer = null;
		mIContainer = null;
		mISamples = null;
		mOSamples = null;
		mIVideoPictures = null;
		mOVideoPictures = null;
		mOCoders = null;
		mICoders = null;
		mASamplers = null;
		mVSamplers = null;
	}

	/**
	 * Allow child class to override this method to alter the audio frame before
	 * it is rencoded and written. In this implementation the audio frame is
	 * passed through unmodified.
	 * 
	 * @param audioFrame
	 *          the source audio frame to be modified
	 * 
	 * @return the modified audio frame
	 */

	protected IAudioSamples alterAudioFrame(IAudioSamples audioFrame)
	{
		return audioFrame;
	}

	/**
	 * Allow child class to override this method to alter the video frame before
	 * it is rencoded and written. In this implementation the video frame is
	 * passed through unmodified.
	 * 
	 * @param videoFrame
	 *          the source video frame to be modified
	 * 
	 * @return the modified video frame
	 */

	protected IVideoPicture alterVideoFrame(IVideoPicture videoFrame)
	{
		return videoFrame;
	}


	private void writePacket(IPacket oPacket) throws ConverterException
	{
		int retval;
		if (oPacket.isComplete())
		{
			if (mRealTimeEncoder)
			{
				delayForRealTime(oPacket);
			}
			/**
			 * If we got a complete packet out of the encoder, then go ahead
			 * and write it to the container.
			 */
			retval = mOContainer.writePacket(oPacket, mForceInterleave);
			if (retval < 0) {				
				throw new ConverterException("could not write output packet");
			}
		}
	}

	/**
	 * WARNING for those who want to copy this method and think it'll stream
	 * for them -- it won't.  It doesn't interleave packets from non-interleaved
	 * containers, so instead it'll write chunky data.  But it's useful if you
	 * have previously interleaved data that you want to write out slowly to
	 * a file, or, a socket.
	 * @param oPacket the packet about to be written.
	 */
	private void delayForRealTime(IPacket oPacket)
	{
		// convert packet timestamp to microseconds
		final IRational timeBase = oPacket.getTimeBase();
		if (timeBase == null || timeBase.getNumerator() == 0 ||
				timeBase.getDenominator() == 0)
			return;
		long dts = oPacket.getDts();
		if (dts == Global.NO_PTS)
			return;

		final long currStreamTime = IRational.rescale(dts,
				1,
				1000000,
				timeBase.getNumerator(),
				timeBase.getDenominator(),
				IRational.Rounding.ROUND_NEAR_INF);
		if (mStartStreamTime == null)
			mStartStreamTime = currStreamTime;

		// convert now to microseconds
		final long currClockTime = System.nanoTime()/1000;
		if (mStartClockTime == null)
			mStartClockTime = currClockTime;

		final long currClockDelta  = currClockTime - mStartClockTime;
		if (currClockDelta < 0)
			return;
		final long currStreamDelta = currStreamTime - mStartStreamTime;
		if (currStreamDelta < 0)
			return;
		final long streamToClockDeltaMilliseconds = (currStreamDelta - currClockDelta)/1000;
		if (streamToClockDeltaMilliseconds <= 0)
			return;
		try
		{
			Thread.sleep(streamToClockDeltaMilliseconds);
		}
		catch (InterruptedException e)
		{
		}
	}

	
	@Override
	public boolean openMedia(String mediaFile, ConverterFormatEnum inputFormat)
			throws ConverterException {
		
		inputURL = mediaFile;
		
		mHasAudio = true;
		mHasVideo = true;

		mRealTimeEncoder = true;

		
		String icontainerFormat = this.getConverterFormat(inputFormat);    
		//String iacodec = cmdLine.getOptionValue("iacodec");
		//int isampleRate = getIntOptionValue(cmdLine, "iasamplerate", 0);
		//int ichannels = getIntOptionValue(cmdLine, "iachannels", 0);

		// Should have everything now!
		int retval = 0;

		/**
		 * Create one container for input, and one for output.
		 */
		mIContainer = IContainer.make();
		
		
		IContainerFormat iFmt = null;
		
		// override input format
		if (icontainerFormat != null)
		{
			iFmt = IContainerFormat.make();

			/**
			 * Try to find an output format based on what the user specified, or
			 * failing that, based on the outputURL (e.g. if it ends in .flv, we'll
			 * guess FLV).
			 */
			retval = iFmt.setInputFormat(icontainerFormat);
			if (retval < 0)
				throw new ConverterException("could not find input container format: " + icontainerFormat);
		}    


		/**
		 * Open the input container for Reading.
		 */
		IMetaData parameters = IMetaData.make();

		//if (isampleRate > 0)
		//	parameters.setValue("sample_rate", ""+isampleRate);

		//if (ichannels > 0)
		//	parameters.setValue("channels", ""+ichannels);

		IMetaData rejectParameters = IMetaData.make();

		retval = mIContainer.open(inputURL, IContainer.Type.READ, iFmt, false, true, 
				parameters, rejectParameters);
		if (retval < 0)
			throw new ConverterException("could not open url: " + inputURL);
		if (rejectParameters.getNumKeys() > 0)
			throw new ConverterException("some parameters were rejected: " + rejectParameters);

				/**
		 * Find out how many streams are there in the input container? For example,
		 * most FLV files will have 2 -- 1 audio stream and 1 video stream.
		 */
		int numStreams = mIContainer.getNumStreams();
		if (numStreams <= 0)
			throw new ConverterException("not streams in input url: " + inputURL);

		

		/**
		 * That's it with setup; we're good to begin!
		 */
		return true;

	}

	@Override
	public boolean openOutput(String mediaFile, ConverterFormatEnum outputFormat)
			throws ConverterException {
		// TODO Auto-generated method stub
		
		outputURL = mediaFile;
		
		int retval;
		
		String containerFormat = this.getConverterFormat(outputFormat);
		
		mOContainer = IContainer.make();
		
		oFmt = null;
		
		/**
		 * If the user EXPLICITLY asked for a output container format, we'll try to
		 * honor their request here.
		 */
		if (containerFormat != null)
		{
			oFmt = IContainerFormat.make();
			/**
			 * Try to find an output format based on what the user specified, or
			 * failing that, based on the outputURL (e.g. if it ends in .flv, we'll
			 * guess FLV).
			 */
			retval = oFmt.setOutputFormat(containerFormat, mediaFile, null);
			if (retval < 0)
				throw new ConverterException("could not find output container format: "
						+ containerFormat);
		}

		/**
		 * Open the output container for writing. If oFmt is null, we are telling
		 * Xuggler to guess the output container format based on the outputURL.
		 */
		retval = mOContainer.open(mediaFile, IContainer.Type.WRITE, oFmt);
		//mOContainer.setFormat(oFmt);
		//retval = mOContainer.open(outputStream, oFmt);
		
		if (retval < 0)
			throw new ConverterException("could not open output url: " + mediaFile);
		
		return true;
	}

	@Override
	public void setupReadStreams(String audioLanguage)
			throws ConverterException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setupWriteStreams(ConverterCodecEnum videoCodec,
			int videoBitrate, ConverterCodecEnum audioCodec, int audioBitrate)
			throws ConverterException {
		// TODO Auto-generated method stub
		
		int astream = -1;
		int aquality = 0;

		int sampleRate = 0;
		int channels = 0;
		int abitrate = audioBitrate;
		int vbitrate = videoBitrate;
		int vbitratetolerance = 0;
		int vquality = -1;
		
		int vstream = -1;
		double vscaleFactor = 1.0;
		
		int retval;

		String acodec = this.getEncoderName(audioCodec);
		String vcodec = this.getEncoderName(videoCodec);
		
		
		/**
		 * Here we create IStream, IStreamCoders and other objects for each input
		 * stream.
		 * 
		 * We make parallel objects for each output stream as well.
		 */
		
		int numStreams = mIContainer.getNumStreams();
		
		mIStreams = new IStream[numStreams];
		mICoders = new IStreamCoder[numStreams];
		mOStreams = new IStream[numStreams];
		mOCoders = new IStreamCoder[numStreams];
		
		mASamplers = new IAudioResampler[numStreams];
		mVSamplers = new IVideoResampler[numStreams];
		
		mIVideoPictures = new IVideoPicture[numStreams];
		mOVideoPictures = new IVideoPicture[numStreams];
		
		mISamples = new IAudioSamples[numStreams];
		mOSamples = new IAudioSamples[numStreams];

		/**
		 * Now let's go through the input streams one by one and explicitly set up
		 * our contexts.
		 */
		for (int i = 0; i < numStreams; i++)
		{
			/**
			 * Get the IStream for this input stream.
			 */
			IStream is = mIContainer.getStream(i);
			/**
			 * And get the input stream coder. Xuggler will set up all sorts of
			 * defaults on this StreamCoder for you (such as the audio sample rate)
			 * when you open it.
			 * 
			 * You can create IStreamCoders yourself using
			 * IStreamCoder#make(IStreamCoder.Direction), but then you have to set all
			 * parameters yourself.
			 */
			IStreamCoder ic = is.getStreamCoder();

			/**
			 * Find out what Codec Xuggler guessed the input stream was encoded with.
			 */
			ICodec.Type cType = ic.getCodecType();

			mIStreams[i] = is;
			mICoders[i] = ic;
			mOStreams[i] = null;
			mOCoders[i] = null;
			mASamplers[i] = null;
			mVSamplers[i] = null;
			mIVideoPictures[i] = null;
			mOVideoPictures[i] = null;
			mISamples[i] = null;
			mOSamples[i] = null;

			if (cType == ICodec.Type.CODEC_TYPE_AUDIO && mHasAudio
					&& (astream == -1 || astream == i))
			{
				/**
				 * First, did the user specify an audio codec?
				 */
				ICodec codec = null;
				if (audioCodec != null)
				{
					/**
					 * Looks like they did specify one; let's look it up by name.
					 */
					codec = ICodec.findEncodingCodecByName(acodec);
					if (codec == null || codec.getType() != cType)
						throw new ConverterException("could not find encoder: " + acodec);

				}
				else
				{
					/**
					 * Looks like the user didn't specify an output coder for audio.
					 * 
					 * So we ask Xuggler to guess an appropriate output coded based on the
					 * URL, container format, and that it's audio.
					 */
					codec = ICodec.guessEncodingCodec(oFmt, null, outputURL, null,
							cType);
					if (codec == null)
						throw new ConverterException("could not guess " + cType
								+ " encoder for: " + outputURL);
				}
				/**
				 * So it looks like this stream as an audio stream. Now we add an audio
				 * stream to the output container that we will use to encode our
				 * resampled audio.
				 */
				IStream os = mOContainer.addNewStream(codec);

				/**
				 * And we ask the IStream for an appropriately configured IStreamCoder
				 * for output.
				 * 
				 * Unfortunately you still need to specify a lot of things for
				 * outputting (because we can't really guess what you want to encode
				 * as).
				 */
				IStreamCoder oc = os.getStreamCoder();

				mOStreams[i] = os;
				mOCoders[i] = oc;

				/**
				 * Now let's see if the codec can support the input sample format; if not
				 * we pick the last sample format the codec supports.
				 */
				Format preferredFormat = ic.getSampleFormat();

				List<Format> formats = codec.getSupportedAudioSampleFormats();
				for(Format format : formats) {
					oc.setSampleFormat(format);
					if (format == preferredFormat)
						break;
				}

				

				/**
				 * In general a IStreamCoder encoding audio needs to know: 1) A ICodec
				 * to use. 2) The sample rate and number of channels of the audio. Most
				 * everything else can be defaulted.
				 */

				/**
				 * If the user didn't specify a sample rate to encode as, then just use
				 * the same sample rate as the input.
				 */
				if (sampleRate == 0)
					sampleRate = ic.getSampleRate();
				oc.setSampleRate(sampleRate);
				/**
				 * If the user didn't specify a bit rate to encode as, then just use the
				 * same bit as the input.
				 */
				if (abitrate == 0)
					abitrate = ic.getBitRate();
				if (abitrate == 0)
					// some containers don't give a bit-rate
					abitrate = 64000;
				oc.setBitRate(abitrate);

				/**
				 * If the user didn't specify the number of channels to encode audio as,
				 * just assume we're keeping the same number of channels.
				 */
				if (channels == 0)
					channels = ic.getChannels();
				oc.setChannels(channels);

				/**
				 * And set the quality (which defaults to 0, or highest, if the user
				 * doesn't tell us one).
				 */
				oc.setGlobalQuality(aquality);

				/**
				 * Now check if our output channels or sample rate differ from our input
				 * channels or sample rate.
				 * 
				 * If they do, we're going to need to resample the input audio to be in
				 * the right format to output.
				 */
				if (oc.getChannels() != ic.getChannels()
						|| oc.getSampleRate() != ic.getSampleRate()
						|| oc.getSampleFormat() != ic.getSampleFormat())
				{
					/**
					 * Create an audio resampler to do that job.
					 */
					mASamplers[i] = IAudioResampler.make(oc.getChannels(), ic
							.getChannels(), oc.getSampleRate(), ic.getSampleRate(),
							oc.getSampleFormat(), ic.getSampleFormat());
					if (mASamplers[i] == null)
					{
						throw new ConverterException(
								"could not open audio resampler for stream: " + i);
					}
				}
				else
				{
					mASamplers[i] = null;
				}
				/**
				 * Finally, create some buffers for the input and output audio
				 * themselves.
				 * 
				 * We'll use these repeated during the #run(CommandLine) method.
				 */
				mISamples[i] = IAudioSamples.make(1024, ic.getChannels(), ic.getSampleFormat());
				mOSamples[i] = IAudioSamples.make(1024, oc.getChannels(), oc.getSampleFormat());
			}
			else if (cType == ICodec.Type.CODEC_TYPE_VIDEO && mHasVideo
					&& (vstream == -1 || vstream == i))
			{
				/**
				 * If you're reading these commends, this does the same thing as the
				 * above branch, only for video. I'm going to assume you read those
				 * comments and will only document something substantially different
				 * here.
				 */
				ICodec codec = null;
				if (vcodec != null)
				{
					codec = ICodec.findEncodingCodecByName(vcodec);
					if (codec == null || codec.getType() != cType)
						throw new ConverterException("could not find encoder: " + vcodec);
				}
				else
				{
					codec = ICodec.guessEncodingCodec(oFmt, null, outputURL, null,
							cType);
					if (codec == null)
						throw new ConverterException("could not guess " + cType
								+ " encoder for: " + outputURL);

				}
				final IStream os = mOContainer.addNewStream(codec);
				final IStreamCoder oc = os.getStreamCoder();

				mOStreams[i] = os;
				mOCoders[i] = oc;


				// Set options AFTER selecting codec
				//final String vpreset = cmdLine.getOptionValue("vpreset");
				//if (vpreset != null)
				//	Configuration.configure(vpreset, oc);

				/**
				 * In general a IStreamCoder encoding video needs to know: 1) A ICodec
				 * to use. 2) The Width and Height of the Video 3) The pixel format
				 * (e.g. IPixelFormat.Type#YUV420P) of the video data. Most everything
				 * else can be defaulted.
				 */
				if (vbitrate == 0)
					vbitrate = ic.getBitRate();
				if (vbitrate == 0)
					vbitrate = 250000;
				oc.setBitRate(vbitrate);
				if (vbitratetolerance > 0)
					oc.setBitRateTolerance(vbitratetolerance);

				int oWidth = ic.getWidth();
				int oHeight = ic.getHeight();

				if (oHeight <= 0 || oWidth <= 0)
					throw new ConverterException("could not find width or height in url: "
							+ inputURL);

				/**
				 * For this program we don't allow the user to specify the pixel format
				 * type; we force the output to be the same as the input.
				 */
				oc.setPixelType(ic.getPixelType());

				if (true)
				{
					/**
					 * In this case, it looks like the output video requires rescaling, so
					 * we create a IVideoResampler to do that dirty work.
					 */
					oWidth = (int) 480;
					oHeight = (int) 270;

					mVSamplers[i] = IVideoResampler
							.make(oWidth, oHeight, oc.getPixelType(), ic.getWidth(), ic
									.getHeight(), ic.getPixelType());
					if (mVSamplers[i] == null)
					{
						throw new ConverterException(
								"This version of Xuggler does not support video resampling "
										+ i);
					}
				}
				else
				{
					mVSamplers[i] = null;
				}
				oc.setHeight(oHeight);
				oc.setWidth(oWidth);

				if (vquality >= 0)
				{
					oc.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
					oc.setGlobalQuality(vquality);
				}

				/**
				 * TimeBases are important, especially for Video. In general Audio
				 * encoders will assume that any new audio happens IMMEDIATELY after any
				 * prior audio finishes playing. But for video, we need to make sure
				 * it's being output at the right rate.
				 * 
				 * In this case we make sure we set the same time base as the input, and
				 * then we don't change the time stamps of any IVideoPictures.
				 * 
				 * But take my word that time stamps are tricky, and this only touches
				 * the envelope. The good news is, it's easier in Xuggler than some
				 * other systems.
				 */
				IRational num = null;
				num = ic.getFrameRate();
				oc.setFrameRate(num);
				oc.setTimeBase(IRational.make(num.getDenominator(), num
						.getNumerator()));
				num = null;

				/**
				 * And allocate buffers for us to store decoded and resample video
				 * pictures.
				 */
				mIVideoPictures[i] = IVideoPicture.make(ic.getPixelType(), ic
						.getWidth(), ic.getHeight());
				mOVideoPictures[i] = IVideoPicture.make(oc.getPixelType(), oc
						.getWidth(), oc.getHeight());
			}
			else
			{
				log.warn("Ignoring input stream {} of type {}", i, cType);
			}

			/**
			 * Now, once you've set up all the parameters on the StreamCoder, you must
			 * open() them so they can do work.
			 * 
			 * They will return an error if not configured correctly, so we check for
			 * that here.
			 */
			if (mOCoders[i] != null)
			{
				// some codecs require experimental mode to be set, and so we set it here.
				retval = mOCoders[i].setStandardsCompliance(IStreamCoder.CodecStandardsCompliance.COMPLIANCE_EXPERIMENTAL);
				if (retval < 0)
					throw new ConverterException ("could not set compliance mode to experimental");

				retval = mOCoders[i].open(null, null);
				if (retval < 0)
					throw new ConverterException(
							"could not open output encoder for stream: " + i);
				retval = mICoders[i].open(null, null);
				if (retval < 0)
					throw new ConverterException(
							"could not open input decoder for stream: " + i);
			}
		}

		
	}

	@Override
	protected void mainLoop() throws ConverterException {
		// TODO Auto-generated method stub
		
		/**
		 * Pretty much every output container format has a header they need written,
		 * so we do that here.
		 * 
		 * You must configure your output IStreams correctly before writing a
		 * header, and few formats deal nicely with key parameters changing (e.g.
		 * video width) after a header is written.
		 */
		int retval = mOContainer.writeHeader();
		if (retval < 0)
			throw new ConverterException("Could not write header ");
		
		 /**
	     * Create packet buffers for reading data from and writing data to the
	     * conatiners.
	     */
	    IPacket iPacket = IPacket.make();
	    IPacket oPacket = IPacket.make();

	    /**
	     * Keep some "pointers' we'll use for the audio we're working with.
	     */
	    IAudioSamples inSamples = null;
	    IAudioSamples outSamples = null;
	    IAudioSamples reSamples = null;

	    /**
	     * And keep some convenience pointers for the specific stream we're working
	     * on for a packet.
	     */
	    IStreamCoder ic = null;
	    IStreamCoder oc = null;
	    IAudioResampler as = null;
	    IVideoResampler vs = null;
	    IVideoPicture inFrame = null;
	    IVideoPicture reFrame = null;

	    /**
	     * Now, we've already opened the files in #setupStreams(CommandLine). We
	     * just keep reading packets from it until the IContainer returns <0
	     */
	    while (mIContainer.readNextPacket(iPacket) >= 0 && !closed)
	    {
	      /**
	       * Find out which stream this packet belongs to.
	       */
	      int i = iPacket.getStreamIndex();
	      int offset = 0;

	      /**
	       * Find out if this stream has a starting timestamp
	       */
	      IStream stream = mIContainer.getStream(i);
	      long tsOffset = 0;
	      if (stream.getStartTime() != Global.NO_PTS && stream.getStartTime() > 0
	          && stream.getTimeBase() != null)
	      {
	        IRational defTimeBase = IRational.make(1,
	            (int) Global.DEFAULT_PTS_PER_SECOND);
	        tsOffset = defTimeBase.rescale(stream.getStartTime(), stream
	            .getTimeBase());
	      }
	      /**
	       * And look up the appropriate objects that are working on that stream.
	       */
	      ic = mICoders[i];
	      oc = mOCoders[i];
	      as = mASamplers[i];
	      vs = mVSamplers[i];
	      inFrame = mIVideoPictures[i];
	      reFrame = mOVideoPictures[i];
	      inSamples = mISamples[i];
	      reSamples = mOSamples[i];

	      if (oc == null)
	        // we didn't set up this coder; ignore the packet
	        continue;

	      /**
	       * Find out if the stream is audio or video.
	       */
	      ICodec.Type cType = ic.getCodecType();

	      if (cType == ICodec.Type.CODEC_TYPE_AUDIO && mHasAudio)
	      {
	        /**
	         * Decoding audio works by taking the data in the packet, and eating
	         * chunks from it to create decoded raw data.
	         * 
	         * However, there may be more data in a packet than is needed to get one
	         * set of samples (or less), so you need to iterate through the byts to
	         * get that data.
	         * 
	         * The following loop is the standard way of doing that.
	         */
	        while (offset < iPacket.getSize())
	        {
	          retval = ic.decodeAudio(inSamples, iPacket, offset);
	          if (retval <= 0) 
	            throw new ConverterException("could not decode audio.  stream: " + i);

	          if (inSamples.getTimeStamp() != Global.NO_PTS)
	            inSamples.setTimeStamp(inSamples.getTimeStamp() - tsOffset);

	          log.trace("packet:{}; samples:{}; offset:{}", new Object[]
	          {
	              iPacket, inSamples, tsOffset
	          });

	          /**
	           * If not an error, the decodeAudio returns the number of bytes it
	           * consumed. We use that so the next time around the loop we get new
	           * data.
	           */
	          offset += retval;
	          int numSamplesConsumed = 0;
	          /**
	           * If as is not null then we know a resample was needed, so we do that
	           * resample now.
	           */
	          if (as != null && inSamples.getNumSamples() > 0)
	          {
	            retval = as.resample(reSamples, inSamples, inSamples
	                .getNumSamples());

	            outSamples = reSamples;
	          }
	          else
	          {
	            outSamples = inSamples;
	          }

	          /**
	           * Include call a hook to derivied classes to allow them to alter the
	           * audio frame.
	           */

	          outSamples = alterAudioFrame(outSamples);

	          /**
	           * Now that we've resampled, it's time to encode the audio.
	           * 
	           * This workflow is similar to decoding; you may have more, less or
	           * just enough audio samples available to encode a packet. But you
	           * must iterate through.
	           * 
	           * Unfortunately (don't ask why) there is a slight difference between
	           * encodeAudio and decodeAudio; encodeAudio returns the number of
	           * samples consumed, NOT the number of bytes. This can be confusing,
	           * and we encourage you to read the IAudioSamples documentation to
	           * find out what the difference is.
	           * 
	           * But in any case, the following loop encodes the samples we have
	           * into packets.
	           */
	          while (numSamplesConsumed < outSamples.getNumSamples())
	          {
	            retval = oc.encodeAudio(oPacket, outSamples, numSamplesConsumed);
	            if (retval <= 0)
	              throw new ConverterException("Could not encode any audio: "
	                  + retval);
	            /**
	             * Increment the number of samples consumed, so that the next time
	             * through this loop we encode new audio
	             */
	            numSamplesConsumed += retval;
	            log.trace("out packet:{}; samples:{}; offset:{}", new Object[]{
	                oPacket, outSamples, tsOffset
	            });

	            writePacket(oPacket);
	          }
	        }

	      }
	      else if (cType == ICodec.Type.CODEC_TYPE_VIDEO && mHasVideo)
	      {
	        /**
	         * This encoding workflow is pretty much the same as the for the audio
	         * above.
	         * 
	         * The only major delta is that encodeVideo() will always consume one
	         * frame (whereas encodeAudio() might only consume some samples in an
	         * IAudioSamples buffer); it might not be able to output a packet yet,
	         * but you can assume that you it consumes the entire frame.
	         */
	        IVideoPicture outFrame = null;
	        while (offset < iPacket.getSize())
	        {
	          retval = ic.decodeVideo(inFrame, iPacket, offset);
	          if (retval <= 0)
	            throw new ConverterException("could not decode any video.  stream: "
	                + i);

	          log.trace("decoded vid ts: {}; pkts ts: {}", inFrame.getTimeStamp(),
	              iPacket.getTimeStamp());
	          if (inFrame.getTimeStamp() != Global.NO_PTS)
	            inFrame.setTimeStamp(inFrame.getTimeStamp() - tsOffset);

	          offset += retval;
	          if (inFrame.isComplete())
	          {
	            if (vs != null)
	            {
	              retval = vs.resample(reFrame, inFrame);
	              if (retval < 0)
	                throw new ConverterException("could not resample video");
	              outFrame = reFrame;
	            }
	            else
	            {
	              outFrame = inFrame;
	            }

	            /**
	             * Include call a hook to derivied classes to allow them to alter
	             * the audio frame.
	             */

	            outFrame = alterVideoFrame(outFrame);

	            outFrame.setQuality(0);
	            retval = oc.encodeVideo(oPacket, outFrame, 0);
	            if (retval < 0)
	              throw new ConverterException("could not encode video");
	            writePacket(oPacket);
	          }
	        }
	      }
	      else
	      {
	        /**
	         * Just to be complete; there are other types of data that can show up
	         * in streams (e.g. SUB TITLE).
	         * 
	         * Right now we don't support decoding and encoding that data, but youc
	         * could still decide to write out the packets if you wanted.
	         */
	        log.trace("ignoring packet of type: {}", cType);
	      }

	    }

	    // and cleanup.
	    closeStreams();

		
	}

	@Override
	protected boolean CanHandle(String inputUrl,
			ConverterFormatEnum inputFormat, String outputUrl,
			ConverterFormatEnum outputFormat) {
		// TODO Auto-generated method stub
		return outputURL.endsWith(".webm");
	}
	
	protected String getEncoderName(ConverterCodecEnum codec) {
		
		if (codec.equals(ConverterCodecEnum.VORBIS)) {
			return "libvorbis";
		}

		if (codec.equals(ConverterCodecEnum.VP8)) {
			return "libvpx";
		}
		return null;
	}
		
	
	protected String getConverterFormat(ConverterFormatEnum format) {
		if (format.equals(ConverterFormatEnum.WEBM))
			return "webm";
		if (format.equals(ConverterFormatEnum.MKV))
			return "matroska";
		return "";
	}

}
