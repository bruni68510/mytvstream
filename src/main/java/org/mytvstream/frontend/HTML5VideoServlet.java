package org.mytvstream.frontend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
	   		
			/**
		     * Retrieve servlet parameters. 
		     */
			int port = new Integer(request.getParameter("port")).intValue();	
		
			
			try {
				
				HttpSession session = request.getSession();
				session.setMaxInactiveInterval(10*60);
						
				response.setContentType("video/webm");
				//response.setContentLength(100000000);
				response.setStatus(200);
				
				// Set standard HTTP/1.1 no-cache headers.
				response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

				// Set standard HTTP/1.0 no-cache header.
				response.setHeader("Pragma", "no-cache");
								
				//PrintWriter writer = response.getWriter();
				
				 
				
				final Socket socket = new Socket("localhost", port);
				
			    final BufferedInputStream inStream = new BufferedInputStream(socket.getInputStream());
			    final BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());
			    final byte[] buffer = new byte[4096];
			    for (int read = inStream.read(buffer); read >= 0; read = inStream.read(buffer))
			        outStream.write(buffer, 0, read);
			    
			    
			    inStream.close();
			    outStream.close();
			   
			    logger.debug("video done");
			    	            
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("IOException in HTML5VideoServlet:" + e.getMessage());
			}
			finally {
			
				 logger.debug("video done");
			}
		    
	}
		
				
}
