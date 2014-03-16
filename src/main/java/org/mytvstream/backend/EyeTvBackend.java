package org.mytvstream.backend;


import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
//import org.apache.commons.httpclient.URI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mytvstream.converter.ConverterFormatEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tvheadend.tvhguide.htsp.HTSMessage;
import org.tvheadend.tvhguide.htsp.HTSResponseHandler;


public class EyeTvBackend extends Backend {

	/**
	 * Default bouquet
	 */
	protected static String bouquetName = "TV and Radios";

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(EyeTvBackend.class);

	/**
	 * Json Parser
	 */
	JSONParser parser = new JSONParser();

	/**
	 * httpclient
	 */
	HttpClient client = new HttpClient();

	@Override	
	public ConverterFormatEnum getDefaultFormat() {
		// TODO Auto-generated method stub
		return ConverterFormatEnum.HLS;
	}

	/**
	 * Connect, check first if connected and then ask for channels.
	 */
	@Override
	public void connect() throws BackendException {
		// TODO Auto-generated method stub
		if (isConnected()) {
			
			// get the channels			
			GetMethod method = new GetMethod("http://" + server + ":" + port + "/live/channels");
			
			try {
			
				// Execute the method.
				int statusCode = client.executeMethod(method);
			
				if (statusCode != HttpStatus.SC_OK) {
					logger.error("Failed to get channels from Eyetv backend " + this.name);
					return;
				}
			
				String responseBody = getResponseBody(method);		 
				Object obj = parser.parse(responseBody);
				JSONObject jsonObject = (JSONObject) obj;
				
				int i = 0;
				
				JSONArray msg = (JSONArray) jsonObject.get("channelList");
				Iterator<JSONObject> iterator = msg.iterator();
				while (iterator.hasNext()) {
				
					JSONObject jsonChannel = (JSONObject)iterator.next();
					
					int id = i++; 
					int number = new Integer((String)jsonChannel.get("displayNumber")).intValue();
					String name = (String)jsonChannel.get("name");
					
					Channel channel = new Channel(name,id,number);					
					channel.setServiceID((String)jsonChannel.get("serviceID"));
					
					logger.debug("Adding channel " + name + " on EyeTV Backend " + server);
					
					bouquets.get(0).channels.add(channel);
				}
				

			} catch (HttpException e) {
				// TODO Auto-generated catch block
				logger.error("Eyetv failed to get channels :" + e.getMessage());
				throw new BackendException("Eyetv failed to get channels");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("Eyetv failed to get channels :" + e.getMessage());
				throw new BackendException("Eyetv failed to get channels");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.error("Eyetv failed to get channels :" + e.getMessage());
				throw new BackendException("Eyetv failed to get channels");
			}
			finally {
				// Release the connection.
				method.releaseConnection();
			}
			
		}
		else {
			bouquets.get(0).channels.clear();
		}
	}

	/**
	 * isConnected sends the /live/status/0 request to the server
	 * and except a json response with isUp property to true.
	 * The reponse is gzip encoded, need to unzip first.
	 */	
	@Override
	public boolean isConnected() {

		GetMethod method = new GetMethod("http://" + server + ":" + port + "/live/status/0");
		
		try {
			// Execute the method.
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				logger.error("Failed to connect to Eyetv backend " + this.name);
				return false;
			}

			// Read the response body.
			String responseBody = getResponseBody(method);		 
			Object obj = parser.parse(responseBody);
			JSONObject jsonObject = (JSONObject) obj;

			return (Boolean)jsonObject.get("isUp");

		} catch (HttpException e) {
			logger.error("Fatal protocol violation: " + e.getMessage());
			return false;
		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			return false;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("Error reading json from server");
			return false;
		} 
		finally {
			// Release the connection.
			method.releaseConnection();
		}  

	}

	/**
	 * Tune the channel and check the channel to be present, eyetv take some time 
	 * to make the channel available
	 */
	public boolean tuneChannel(BackendListener listener, String channelURL) throws BackendException {
		
		int statusCode;
		int retry = 5;
		GetMethod method = new GetMethod(channelURL);
		
		listener.onMessage("Channel url retrieved waiting for channel to tune");
		
		try{			
			
			do {
				statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {					
					Thread.sleep(4000);
					listener.onMessage("Eyetv backend " + this.name + " channel still not present, retrying") ;
				}
				else {
					return true;
				}
			} while (statusCode != HttpStatus.SC_OK && retry-- > 0);
		
			return false;
			
		} catch (HttpException e) {
			logger.error("Fatal protocol violation: " + e.getMessage());
			throw new BackendException("Failed to tune channel");
		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			throw new BackendException("Failed to tune channel");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error("Interrupted Exception");
			throw new BackendException("Failed to tune channel");
		}
		finally {
			// Release the connection.
			method.releaseConnection();
		}
		
		
	}
	
	/**
	 * Tune the channel and return the HLS URL for the tuned channel.
	 * @param channel
	 * @return
	 */
	@Override
	public synchronized String getChannelUrl(final int channelID) {

		Channel channel = this.bouquets.get(0).channels.get(channelID);
	
		logger.debug("Calling tune url for channel " + channelID);
		
		GetMethod method = new GetMethod("http://" + server + ":" + port + "/live/tuneto/0/" + channel.serviceID);
		
		try {
			// Execute the method.			
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				logger.error("Failed to tune Eyetv backend " + this.name);
				return "";
			}

			// Read the response body.
			String responseBody = getResponseBody(method);		 
			Object obj = parser.parse(responseBody);
			JSONObject jsonObject = (JSONObject) obj;

			String m3u8 = (String)jsonObject.get("m3u8URL");
						
			String url = "http://" + server + ":" + port + "/live/stream/" + m3u8;
			
			return url;
			
		} catch (HttpException e) {
			logger.error("Fatal protocol violation: " + e.getMessage());
			return null;
		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("Error reading json from server");
			return null;
		} 
		finally {
			// Release the connection.
			method.releaseConnection();
		}  
	}
	
	
	/**
	 * helper handling gziped contents
	 */
	protected String getResponseBody(HttpMethod method) throws IOException{
		Header contentEncoding = method.getResponseHeader("Content-Encoding");

		if(contentEncoding !=  null ){
			String acceptEncodingValue = contentEncoding.getValue();
			if(acceptEncodingValue.indexOf("gzip") != -1){	      
				StringWriter responseBody = new StringWriter();
				PrintWriter responseWriter = new PrintWriter(responseBody);
				GZIPInputStream zippedInputStream =  new GZIPInputStream(method.getResponseBodyAsStream());
				BufferedReader r = new BufferedReader(new InputStreamReader(zippedInputStream));
				String line = null;
				while( (line =r.readLine()) != null){
					responseWriter.println(line);
				}
				return responseBody.toString();
			}
		}

		return method.getResponseBodyAsString();
	}

	/**
	 * Constructor, registering name and default bouquet.
	 * @param id
	 * @param backendConfiguration
	 */
	public EyeTvBackend(int id, org.mytvstream.configuration.Configuration.Backends.Backend backendConfiguration) 
	{

		super(id,backendConfiguration);

		bouquets.add(new Bouquet(0,bouquetName,0));		
		name = "EyeTV Backend " + backendConfiguration.getServer();
	}

}
