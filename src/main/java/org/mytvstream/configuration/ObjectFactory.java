//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.03.14 at 11:40:58 PM CET 
//


package org.mytvstream.configuration;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.mytvstream.configuration package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.mytvstream.configuration
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Configuration.Backends }
     * 
     */
    public Configuration.Backends createConfigurationBackends() {
        return new Configuration.Backends();
    }

    /**
     * Create an instance of {@link Configuration }
     * 
     */
    public Configuration createConfiguration() {
        return new Configuration();
    }

    /**
     * Create an instance of {@link Configuration.Client }
     * 
     */
    public Configuration.Client createConfigurationClient() {
        return new Configuration.Client();
    }

    /**
     * Create an instance of {@link Configuration.Backends.Backend }
     * 
     */
    public Configuration.Backends.Backend createConfigurationBackendsBackend() {
        return new Configuration.Backends.Backend();
    }

}
