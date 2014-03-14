package org.mytvstream.main;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;

// JAXB imports
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

// jetty imports
import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

// mytvstream imports
import org.mytvstream.backend.Backend;
import org.mytvstream.backend.BackendException;
import org.mytvstream.backend.BackendFactory;
import org.mytvstream.configuration.Configuration;
import org.mytvstream.configuration.Configuration.Client;
import org.mytvstream.converter.ConverterException;
import org.mytvstream.producerserver.ProducerServer;
import org.mytvstream.producerserver.ProducerServerException;
import org.mytvstream.producerserver.ProducerServerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Main {

	/**
	 * Singleton definition
	 */
	private static Main SINGLETON = new Main();
	private Main() {}
	static public Main getInstance() {
		return SINGLETON;
	}
	
	/**
	 * Internal storage of backend
	 * 
	 */
	protected ArrayList<Backend> registeredBackend = new ArrayList<Backend>();
	
	public ArrayList<Backend> getBackend() {
		return registeredBackend;
	}
	
	/**
	 * Internal variables of embedded jetty server
	 */
	private static final int serverPort = 8085;
    private static final String WEBROOT_INDEX = "/webroot/";
	private Server server;
	private URI serverURI;
	
	/**
	 * Internal variable of embedded flazr server
	 */
	private ProducerServer producerServer;
	
	
	/**
	 * Logger defintion
	 */
	private Logger logger = LoggerFactory.getLogger(Main.class);
	
	/**
	 * register Backend in the main class based on the given configuration file 
	 * @param configurationFile : File used as configuration of backends
	 * @return : success or failure
	 * @throws BackendException
	 * @throws JAXBException
	 */
	protected boolean registerBackend(File configurationFile) throws BackendException, JAXBException 
	{
		/**
		 * Reads the configuration file.
		 */
		JAXBContext jc = JAXBContext.newInstance("org.mytvstream.configuration");
		Unmarshaller u = jc.createUnmarshaller();
		Configuration c = (Configuration)u.unmarshal( configurationFile );
		
		/**
		 * Creates the backends for that configuration.
		 */
		int i = 0;
		Iterator<org.mytvstream.configuration.Configuration.Backends.Backend> iterator = c.getBackends().getBackend().iterator();
		while(iterator.hasNext()) {
			org.mytvstream.configuration.Configuration.Backends.Backend backendConfiguration = (org.mytvstream.configuration.Configuration.Backends.Backend)iterator.next();					
			Backend backend = BackendFactory.getInstance().getBackend(i++,backendConfiguration.getType(), backendConfiguration);
			
			registeredBackend.add(backend);
		}
		
		return true;
	}
	
	protected boolean registerClientProducer(File configurationFile) throws ProducerServerException, JAXBException {
	
		JAXBContext jc = JAXBContext.newInstance("org.mytvstream.configuration");
		Unmarshaller u = jc.createUnmarshaller();
		Configuration c = (Configuration)u.unmarshal( configurationFile );
		
		Client client = c.getClient();
		
		producerServer = ProducerServerFactory.getInstance().getProducerServer(client.getProducerserver());
		
		producerServer.setDaemon(true);
		producerServer.start();
		
		return true;
	}
	
	
	protected boolean startJetty() throws Exception {
		server = new Server();
		
		SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(serverPort);
        server.addConnector(connector);
		
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null)
        {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        
     // Points to wherever /webroot/ (the resource) is
        URI baseUri = indexUri.toURI();

        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(),"embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }

        // Set JSP to use Standard JavaC always
        System.setProperty("org.apache.jasper.compiler.disablejsr199","false");

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setAttribute("javax.servlet.context.tempdir",scratchDir);
        context.setResourceBase(baseUri.toASCIIString());
        server.setHandler(context);
        
        // Add Application Servlets
        //context.addServlet(DateServlet.class,"/date/");

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        context.setClassLoader(jspClassLoader);

        // Add JSP Servlet (must be named "jsp")
        ServletHolder holderJsp = new ServletHolder("jsp",JspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel","DEBUG");
        holderJsp.setInitParameter("fork","false");
        holderJsp.setInitParameter("xpoweredBy","false");
        //holderJsp.setInitParameter("compilerTargetVM","1.7");
        //holderJsp.setInitParameter("compilerSourceVM","1.7");
        holderJsp.setInitParameter("keepgenerated","true");
        context.addServlet(holderJsp,"*.jsp");
        context.addServlet(holderJsp,"*.jspf");
        context.addServlet(holderJsp,"*.jspx");

        // Add Default Servlet (must be named "default")
        ServletHolder holderDefault = new ServletHolder("default",DefaultServlet.class);
        logger.info("Base URI: " + baseUri);
        holderDefault.setInitParameter("resourceBase",baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed","true");
        context.addServlet(holderDefault,"/");

        // Add a websocket to a specific path spec
        ServletHolder holderEvents = new ServletHolder("ws-events", MainWebSocketServlet.class);
        context.addServlet(holderEvents, "/events/*");
        
        // Start Server
        server.start();

        
		return true;
	}
	
	
	protected void shutdown() {
		if (server != null) {
			try {
				server.stop();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
		
		
	} 
	
	public static void main(String args[])  
	{
		
		if (args.length < 1) {
			System.err.println("usage " + Main.class.getName() + " path_to_configuration.xml");
			System.exit(-1);
		}

		try{
			
			/**
			 * Initialized backends
			 */
			Main.getInstance().registerBackend(new File(args[0]));
			
		
			/**
			 * Initializes jetty
			 */
			Main.getInstance().startJetty();
			
			/**
			 * Initialize flazr
			 */
			Main.getInstance().registerClientProducer(new File(args[0]));
			
			Main.getInstance().server.join();
			
		}		
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();					
		}
		finally {
			
			System.out.println("Shutting down");
			
			Main.getInstance().shutdown();
		}
	}
	
}
