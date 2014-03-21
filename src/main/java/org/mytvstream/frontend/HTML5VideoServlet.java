package org.mytvstream.frontend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mytvstream.backend.Backend;
import org.mytvstream.backend.BackendException;
import org.mytvstream.backend.BackendListener;
import org.mytvstream.backend.Channel;
import org.mytvstream.configuration.Configuration;
import org.mytvstream.converter.Converter;
import org.mytvstream.converter.ConverterCodecEnum;
import org.mytvstream.converter.ConverterException;
import org.mytvstream.converter.ConverterFormatEnum;
import org.mytvstream.converter.XugglerConverter;
import org.mytvstream.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * The HTML5 servlet is responsible to generate HTML5 video
 * in format OGG/WEBM. MP4 is not possible because this format is not streamable
 * @author cbrunner
 *
 */
public class HTML5VideoServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -934716386316800940L;

	private static final Logger logger = LoggerFactory.getLogger(HTML5VideoServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
	   		
				
		try 
		{

			HttpSession session = request.getSession();
			session.setMaxInactiveInterval(10*60);
					
			response.setContentType("application/webm");
			response.setStatus(200);
			//response.setContentLength(100000);
			response.flushBuffer();
			
		    /**
		     * Retrieve servlet parameters. 
		     */
			int backendNr = new Integer(request.getParameter("backend")).intValue();
			long bouquetNr = new Integer(request.getParameter("bouquet")).longValue();
			long channelNr = new Integer(request.getParameter("channel")).longValue();

			
			/**
			 * Get the backend
			 */
			Backend backend = Main.getInstance().getBackend().get(backendNr);
		
			/**
			 * Get the channel
			 */
			
			Channel channel = backend.getChannelByID(backend.getBouquetByID((int)bouquetNr), (int)channelNr);
			
			logger.debug("Get Channel URL");
			
			String inputUrl = backend.getChannelUrl(new BackendListener() {
				@Override
				public void onMessage(String message) {}
				
			}, channel);
			
			logger.debug("tune Channel");
			
			backend.tuneChannel(new BackendListener() {
				@Override
				public void onMessage(String message) {}
				
			}, channel);
			
			Converter converter = new XugglerConverter();
			
			converter.openMedia(inputUrl, backend.getDefaultFormat());
			
			converter.openOutput(response.getOutputStream(), ConverterFormatEnum.WEBM);
			
			converter.setupReadStreams("fre");
			
			Configuration configuration = Main.getInstance().getConfiguration();
			
			ConverterCodecEnum audiocodec = ConverterCodecEnum.VORBIS;
			ConverterCodecEnum videocodec = ConverterCodecEnum.VP8;			
			
			converter.setupWriteStreams(
				videocodec, 
				configuration.getClient().getVideobitrate().intValue(), 
				audiocodec, 
				configuration.getClient().getAudiobitrate().intValue()
			);
		
			converter.start();
			
			converter.join();
			
			logger.debug("Converter done");
		}
		
		catch(BackendException e) {
			logger.error("Backend exception: "+ e.getMessage());
		} catch (ConverterException e) {
			// TODO Auto-generated catch block
			logger.error("Converter exception: "+ e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Servlet IO Exception : " +e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error("Interrupted Exception : " +e.getMessage());
		}
		
	}
	
	
}
