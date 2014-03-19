package org.mytvstream.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
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
	CloseableHttpClient client;

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
			
						
			try {
			
				HttpGet httpget = new HttpGet("http://" + server + ":" + port + "/live/channels");
			
				CloseableHttpResponse response = client.execute(httpget);				
				
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					throw new BackendException("Failed to connect to backend");
				}
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				
				response.close();
				
				Object obj = parser.parse(reader);
				
				
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
				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("Eyetv failed to get channels :" + e.getMessage());
				throw new BackendException("Eyetv failed to get channels");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.error("Eyetv failed to get channels :" + e.getMessage());
				throw new BackendException("Eyetv failed to get channels");
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
		
		try {
			// Execute the method.
			HttpGet httpget = new HttpGet("http://" + server + ":" + port + "/live/status/0");
						
			
			CloseableHttpResponse response = client.execute(httpget);				
			
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return false;
			}

			// Read the response body.
			String responseBody = getResponseBody(response);
			
			response.close();
			
			Object obj = parser.parse(responseBody);
			JSONObject jsonObject = (JSONObject) obj;

			return (Boolean)jsonObject.get("isUp");

		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			return false;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("Error reading json from server");
			return false;
		} 
		  

	}

	/**
	 * Tune the channel and check the channel to be present, eyetv take some time 
	 * to make the channel available
	 */
	public boolean tuneChannel(BackendListener listener, Channel channel) throws BackendException {
		
		int statusCode;
		int retry = 5;
				
		HttpGet httpget = new HttpGet(channel.currentURL);
		
					
		listener.onMessage("Channel url retrieved waiting for channel to tune");
		
		try{			
			
			do {
				CloseableHttpResponse response = client.execute(httpget);

				statusCode = response.getStatusLine().getStatusCode();
				
				response.close();
				
				if (statusCode != HttpStatus.SC_OK) {					
					Thread.sleep(4000);
					listener.onMessage("Eyetv backend " + this.name + " channel still not present, retrying") ;
				}
				else {
					return true;
				}
			} while (statusCode != 200 && retry-- > 0);
		
			return false;
			
		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			throw new BackendException("Failed to tune channel");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error("Interrupted Exception");
			throw new BackendException("Failed to tune channel");
		}
				
		
	}
	
	/**
	 * Tune the channel and return the HLS URL for the tuned channel.
	 * @param channel
	 * @return
	 */
	@Override
	public synchronized String getChannelUrl(BackendListener listener, Channel channel) {

			
		logger.debug("Get channel URL for channel " + channel.getName());
		
		HttpGet httpget = new HttpGet("http://" + server + ":" + port + "/live/tuneto/0/" + channel.serviceID);
				
		
		try {
			// Execute the method.	
			
			CloseableHttpResponse response = client.execute(httpget);

			int statusCode = response.getStatusLine().getStatusCode();
			

			if (statusCode != HttpStatus.SC_OK) {
				logger.error("Failed to tune Eyetv backend " + this.name);
				return "";
			}

			// Read the response body.
			String responseBody = getResponseBody(response);
			
			response.close();
			
			Object obj = parser.parse(responseBody);
			JSONObject jsonObject = (JSONObject) obj;

			String m3u8 = (String)jsonObject.get("m3u8URL");
						
			String url = "http://" + server + ":" + port + "/live/stream/" + m3u8;
			
			channel.currentURL = url;
			
			return url;
			
		} catch (IOException e) {
			logger.error("Fatal transport error: " + e.getMessage());
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("Error reading json from server");
			return null;
		} 
		  
	}
	
	
	/**
	 * helper handling gziped contents
	 */
	protected String getResponseBody(CloseableHttpResponse response) throws IOException{
		//Header contentEncoding = method.getResponseHeader("Content-Encoding");
		
		org.apache.http.Header contentEncoding = response.getLastHeader("Content-Encoding");

		if(contentEncoding !=  null ){
			String acceptEncodingValue = contentEncoding.getValue();
			if(acceptEncodingValue.indexOf("gzip") != -1){	      
				StringWriter responseBody = new StringWriter();
				PrintWriter responseWriter = new PrintWriter(responseBody);
				GZIPInputStream zippedInputStream =  new GZIPInputStream(response.getEntity().getContent());
				BufferedReader r = new BufferedReader(new InputStreamReader(zippedInputStream));
				String line = null;
				while( (line =r.readLine()) != null){
					responseWriter.println(line);
				}
				return responseBody.toString();
			}
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		 
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		
		return result.toString();
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
		
		int timeout = 5;
		RequestConfig config = RequestConfig.custom()
		  //.setConnectTimeout(timeout * 1000)
		  //.setConnectionRequestTimeout(timeout * 1000)
		  .setSocketTimeout(timeout * 1000).build();
		
		client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}

}
