//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.28 at 08:56:09 PM EST 
//

package com.trireplicator.trainingpeaks;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.trireplicator.trainingpeaks.api package. 
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
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.trireplicator.trainingpeaks.api
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Workout }
     * 
     */
    public Workout createWorkout() {
        return new Workout();
    }

    /**
     * Create an instance of {@link ArrayOfWorkout }
     * 
     */
    public ArrayOfWorkout createArrayOfWorkout() {
        return new ArrayOfWorkout();
    }

}
