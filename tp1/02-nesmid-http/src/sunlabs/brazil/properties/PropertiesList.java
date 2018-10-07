/*
 * PropertiesList.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 2002 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version 
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 * 
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.0.
 * The Initial Developer of the Original Code is: drach.
 * Portions created by drach are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): drach, suhler.
 *
 * Version:  1.4
 * Created by drach on 02/04/29
 * Last modified by suhler on 02/05/17 13:59:37
 */

package sunlabs.brazil.properties;

import java.util.Dictionary;
import java.util.Properties;

/* The 1.1 PropertiesList is identical to the BasePropertiesList */

/**
 * A <code>PropertiesList</code> instance is intended to be an element of
 * a doubly linked list consisting of other <code>PropertiesList</code>
 * instances.  Each <code>PropertiesList</code> instance "wraps" a
 * <code>Dictionary</code> object.  A <code>PropertiesList</code> is a
 * subclass of <code>Properties</code> and therefore provides the same
 * API, including the methods and fields of <code>Dictionary</code> and
 * <code>Hashtable</code>.  The <code>PropertiesList</code> class
 * overrides all methods of the <code>Properties</code> API and delegates
 * the method evaluation to the wrapped <code>Properties</code> object.
 * <p>
 * The linked list of <code>PropertiesList</code> objects is constructed
 * by <code>Request</code> for each incoming request.  That is, there is
 * a unique <code>PropertiesList</code> linked list for each request.
 * The head of the initial list constructed by <code>request</code> is
 * <code>Request.props</code> and the tail of the two element list is
 * <code>Request.serverProps</code>.  The former wraps an empty
 * <code>Properties</code> object, while the latter wraps
 * <code>Server.props</code>.  Other <code>PropertiesList</code> objects
 * can be added, and removed, from this initial list as required.
 * <p>
 * Given a reference to a <code>PropertiesList</code> object on the
 * linked list (e.g. <code>request.props</code>), one typically "looks
 * up" the value associated with a name using the
 * <code>getProperty</code> method, which delegates to the wrapped
 * <code>Properties.getProperty</code> method.  If the result is
 * <code>null</code>, meaning the name/value pair is not stored in the
 * wrapped <code>Properties</code> object, the request is "forwarded" to
 * the next object on the linked list, and so on until either the
 * name/value pair is found (and the value is returned) or the end of the
 * list is reached (and <code>null</code> is returned).
 * <p>
 * It may be desirable for the name/value lookup to be delayed until
 * after the lookup request has been passed on to subsequent objects on
 * the list.  This can be done by using the two parameter constructor and
 * setting the second, boolean, parameter to <code>true</code>.  Then the
 * <code>getProperty</code> request is forwarded to the next object in
 * the list rather than delegated to the wrapped <code>Properties</code>
 * object.  If the result of the forwarded request is <code>null</code>,
 * the request is then passed to the wrapped <code>Properties</code>
 * object and it's result is returned.
 * 
 * @author	Steve Drach &lt;drach@sun.com&gt;
 * @version     1.4, 02/05/17
 *
 * @see java.util.Dictionary
 * @see java.util.Hashtable
 * @see java.util.Properties
 */
public class PropertiesList extends BasePropertiesList {

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * an empty new <code>Properties</code> object.
     */
    public PropertiesList() {
	if (debug) {
	    log("*** PL@" + id(this) + " created " + caller());
	}
	wrapped = new Properties();
    }

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * the input <code>Dictionary</code>.
     *
     * @param dict        The <code>Dictionary</code> object wrapped
     *                    by this <code>PropertiesList</code>.
     */
    public PropertiesList(Dictionary dict) {
	if (debug) {
	    log("*** PL@" + id(this) + " created with dict " + id(dict)
		+ " " + caller());
	}
	wrapped = dict;
    }

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * the input <code>Dictionary</code>.  If the boolean parameter
     * is set <code>true</code>, the wrapped <code>Dictionary</code>
     * is searched after subsequent <code>PropertiesList</code>
     * objects in the linked list are searched, and only if the
     * result of that search was <code>null</code>.
     *
     * @param dict             The <code>Dictionary</code> object wrapped
     *                         by this <code>PropertiesList</code>.
     * 
     * @param searchNextFirst  If <code>true</code> all the following
     *                         objects in the list are searched before
     *                         this one.
     */
    public PropertiesList(Dictionary dict, boolean searchNextFirst) {
	this(dict);
	this.searchNextFirst = searchNextFirst;
    }
}
