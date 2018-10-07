/*
 * BasePropertiesList.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 2001-2002 Sun Microsystems, Inc.
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
 * Version:  1.10
 * Created by drach on 01/08/03
 * Last modified by drach on 02/05/17 09:42:54
 */

package sunlabs.brazil.properties;
import nesmid.util.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import sunlabs.brazil.util.Glob;


/**
 * The <code>BasePropertiesList</code> is the abstract superclass for
 * a <code>PropertiesList</code> class.  It is not intended to be 
 * instantiated or created by any method.  Therefore the following
 * documentation discusses the <code>PropertieList</code> class, which
 * is intended to be instantiated.
 * <p>
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
 * @version     1.10, 02/05/17
 *
 * @see java.util.Dictionary
 * @see java.util.Hashtable
 * @see java.util.Properties
 */
public abstract class BasePropertiesList extends Properties {

    protected Dictionary wrapped;
    protected boolean searchNextFirst;

    protected PropertiesList next, prior;

    protected BasePropertiesList() {}

    /**
     * Set <code>true</code> to turn on debug output.  It's alot
     * of output and probably of use only to the author.  Note,
     * if <code>server.props</code> contains the name <code>debugProps</code>
     * this variable will be set <code>true</code> by <code>Server</code>.
     */
    public static boolean debug;

    /**
     * Returns the <code>Dictionary</code> object wrapped by this
     * <code>PropertiesList</code>.
     */
    public Dictionary getWrapped() {
	return wrapped;
    }

    /**
     * Adds this <code>PropertiesList</code> object into
     * a linked list following the object referenced by
     * the <code>cursor</code> parameter.  The result is
     * a list that could look like:
     *
     * request.props -> cursor -> this -> serverProps
     *
     * @param cursor      The list object that will precede this object.
     */
    public void addAfter(PropertiesList cursor) {
	if (cursor != null) {
	    next = cursor.next;
	    if (next != null) {
		next.prior = (PropertiesList)this;
	    }
	    prior = cursor;
	    cursor.next = (PropertiesList)this;
	}
	if (debug) {
	    log("*** addAfter " + cursor.toString());
	    getHead().dump(true, null);
	}
    }

    /**
     * Adds this <code>PropertiesList</code> object into
     * a linked list preceding the object referenced by
     * the <code>cursor</code> parameter.  The result is
     * a list that could look like:
     *
     * request.props -> this -> cursor -> serverProps
     *
     * @param cursor      The list object that will succede this object.
     */
    public void addBefore(PropertiesList cursor) {
	if (cursor != null) {
	    prior = cursor.prior;
	    if (prior != null) {
		prior.next = (PropertiesList)this;
	    }
	    next = cursor;
	    cursor.prior = (PropertiesList)this;
	}
	if (debug) {
	    log("*** addBefore " + cursor.toString());
	    getHead().dump(true, null);
	}
    }

    /**
     * Remove this object from the list in which it's a member.
     *
     * @return            <code>true</code>.
     */
    public boolean remove() {
	PropertiesList head = null;
	if (debug) {
	    if ((head = getHead()) == (PropertiesList)this) {
		head = head.next;
	    }
	}
	if (next != null) {
	    next.prior = prior;
	}
	if (prior != null) {
	    prior.next = next;
	}
	next = prior = null;
	if (debug) {
	    log("*** remove " + toString());
	    head.dump(true, null);
	}
	return true;
    }

    /**
     * Returns the <code>PropertiesList</code> object that succedes this
     * object on the list of which this object is a member.
     *
     * @return            A <code>PropertiesList</code> object or 
     *                    <code>null</code>.
     */
    public PropertiesList getNext() {
	if (debug) {
	    log("*** getNext: PropertyList@" + id(next));
	}
	return next;
    }

    /**
     * Returns the <code>PropertiesList</code> object that precedes this
     * object on the list of which this object is a member.
     *
     * @return            A <code>PropertiesList</code> object or
     *                    <code>null</code>.
     */
    public PropertiesList getPrior() {
	if (debug) {
	    log("*** getPrior: PropertyList@" + id(prior));
	}
	return prior;
    }

    /**
     * Returns the <code>PropertiesList</code> object that is the first object
     * on the list of which this object is a member.  Note that the first
     * object may be this object.
     *
     * @return            A <code>PropertiesList</code> object.
     */
    public PropertiesList getHead() {
	PropertiesList head = (PropertiesList)this;
	while (head.prior != null) {
	    head = head.prior;
	}
	return head;
    }

    /**
     * Find the first <code>PropertiesList</code> object on the list of which
     * this object is a member that wraps the <code>Dictionary</code>
     * parameter.
     *
     * @param d           The <code>Dictionary</code> that is compared with the
     *                    wrapped <code>Dictionary</code>'s for a match.
     *
     * @return            <code>PropertiesList</code> object that wraps the
     *                    input parameter, otherwise <code>null</code>.
     */
    public PropertiesList wraps(Dictionary d) {
	PropertiesList cursor = getHead();
	do {
	    if (cursor.wrapped == d) {
		return cursor;
	    }
	    cursor = cursor.next;
	} while (cursor != null);
	return null;
    }

    /**
     * Starting with this object, print the contents of this and
     * succeeding objects that are on the same list as this object
     * is.
     *
     * @param full        If <code>true</code> also print the contents of the
     *                    wrapped <code>Dictionary</code> object.
     *
     * @param msg         If not <code>null</code>, add this message to the 
     *                    header line.
     */
    public void dump(boolean full, String msg) {
	boolean debug = this.debug;
	this.debug = full;
	if (msg == null) {
	    log("***\ndumping PropertiesList");
	} else {
	    log("***\ndumping PropertiesList " + msg);
	}
	dump2();
	this.debug = debug;
    }

    protected void dump2() {
	log("-----\n" + toString());
	if (next != null) {
	    next.dump2();
	}
    }

    /*
     * Dictionary methods
     */

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Enumeration elements() {
	return wrapped.elements();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object get(Object key) {
	return wrapped.get(key);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public boolean isEmpty() {
	return wrapped.isEmpty();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Enumeration keys() {
	return wrapped.keys();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object put(Object key, Object value) {
	if (debug) {
	    log("*** PL@" + id(this) + " put(" + key + ", " + value + ")");
	}
	return wrapped.put(key, value);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object remove(Object key) {
	return wrapped.remove(key);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public int size() {
	return wrapped.size();
    }

    /*
     * Hashtable methods
     */

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized void clear() {
	((Hashtable)wrapped).clear();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized Object clone() {
	return ((Hashtable)wrapped).clone();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized boolean contains(Object value) {
	return ((Hashtable)wrapped).contains(value);
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized boolean containsKey(Object key) {
	return ((Hashtable)wrapped).containsKey(key);
    }

    /**
     * Returns a <code>String</code> containing the
     * <code>System.identityHashCode</code>s of this object, the wrapped
     * object, and the preceding and succeding objects on the list of
     * which this object is a member.  Additionally, if <code>debug</code>
     * is <code>true</code>, the result of invoking <code>toString</code>
     * on the wrapped <code>Dictionary</code> is appended.
     *
     * @return            <code>String</code> representation of this object.
     */
    public synchronized String toString() {
	StringBuffer sb = new StringBuffer("PropertiesList@").append(id(this));
	sb.append("\n    next: ").append(id(next));
	sb.append("\n    prior: ").append(id(prior));
	sb.append("\n    wrapped: ").append(id(wrapped));
	if (debug) {
	    sb.append("\n    ").append(wrapped.toString());
	}
	return sb.toString();
    }

    // There is no rational way to override rehash(), so it's not here

    /*
     * Properties methods
     */

    /**
     * Looks up <code>key</code> in the wrapped object.  If the result is
     * <code>null</code> and the wrapped object is a <code>Properties</code>
     * object, the request is forwarded to the succeeding object in the list
     * of which this object is a member.  If the search order was changed
     * by constructing this object with the two parameter constructor, the
     * request is first forwarded (if the wrapped object is a
     * <code>Properties</code> object), and then, if the result of the
     * forwarded request is <code>null</code>, the <code>key</code> is looked
     * up in the wrapped <code>Properties</code> object.
     *
     * @param key         The key whose value is sought.
     *
     * @return            The value or <code>null</code>.
     */
    public String getProperty(String key) {
	String value = null;
	if (wrapped instanceof Properties) {
	    if (searchNextFirst) {
		if (next != null) {
		    value = next.getProperty(key);
		}
		if (value == null) {
		    value = ((Properties)wrapped).getProperty(key);
		}
	    } else {
		value = ((Properties)wrapped).getProperty(key);
		if (value == null) {
		    if (next != null) {
			value = next.getProperty(key);
		    }
		}
	    }
	} else {
	    try {
		value = (String)get(key);
	    } catch (ClassCastException e) {}
	}
	if (debug) {
	    log("*** PL@" + id(this) + " getProperty(" + key + ") => " + value);
	}
	return value;
    }

    /**
     * Uses <code>getProperty(String)</code> to look up the value associated
     * with the key.  If the result is <code>null</code>, returns the default
     * value.
     *
     * @param key          The key whose value is sought.
     *
     * @param defaultValue The default value.
     *
     * @return             The value or <code>null</code>.
     */
    public String getProperty(String key, String defaultValue) {
	String value = getProperty(key);
	if (value == null) {
	    value = defaultValue;
	}
	if (debug) {
	    log("*** PL@" + id(this) + " get(" + key + ") => " + value);
	}
	return value;
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public void list(PrintStream out) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).list(out);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public void list(PrintWriter out) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).list(out);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public synchronized void load(InputStream in) throws IOException {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).load(in);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public Enumeration propertyNames() {
	Hashtable h = new Hashtable();
	enumerate(h);
	return h.keys();
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public synchronized void save(OutputStream out, String header) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).save(out, header);
	}
    }

    /*
     * Additional method on wrapped object
     */
    
    /**
     * Returns an <code>Enumeration</code> of property names that
     * match a <code>glob</code> pattern.
     *
     * @param pattern     The <code>glob</code> pattern to match.
     *
     * @return            An <code>Enumeration</code> containing
     *                    matching property names, if any.
     */
    public Enumeration propertyNames(String pattern) {
	Hashtable h = new Hashtable();
	enumerate(h, pattern);
	return h.keys();
    }

    /*
     * Helper methods
     */

    protected synchronized void enumerate(Hashtable h) {
	if (next != null && ! searchNextFirst) {
	    next.enumerate(h);
	}
	if (wrapped instanceof Properties) {
	    Enumeration e = ((Properties)wrapped).propertyNames();
	    while (e.hasMoreElements()) {
		String s = null;
		try {
		    s = (String)e.nextElement();
		} catch (ClassCastException x) {}
		h.put(s, s);
	    }
	}
	if (next != null && searchNextFirst) {
	    next.enumerate(h);
	}
    }
	
    protected synchronized void enumerate(Hashtable h, String pattern) {
	if (next != null && ! searchNextFirst) {
	    next.enumerate(h, pattern);
	}
	if (wrapped instanceof Properties) {
	    Enumeration e = ((Properties)wrapped).propertyNames();
	    while (e.hasMoreElements()) {
		String s = null;
		try {
		    s = (String)e.nextElement();
		} catch (ClassCastException x) {}
		
		
		 if (Glob.match(pattern, s)) {
		    h.put(s, s);
		}
	    }
	}
	if (next != null && searchNextFirst) {
	    next.enumerate(h, pattern);
	}
    }

    protected String id(Object o) {
	if (o == null) {
	    return "null";
	}
	return Integer.toHexString(System.identityHashCode(o));
    }

    protected String caller() {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintWriter out = new PrintWriter(baos);
	(new Throwable()).printStackTrace(out);
	try {
	    out.close();
	    BufferedReader in = new BufferedReader(
		                new InputStreamReader(
			        new ByteArrayInputStream(baos.toByteArray())));
	    String line = null;
	    boolean found = false;
	    while ((line = in.readLine()) != null) {
		if (line.indexOf("PropertiesList.<init>") != -1) {
		    found = true;
		    continue;
		}
		if (found) {
		    return line.trim();
		}
	    }
	} catch (IOException e) {}
	return "";
    }

    // Can't use server.log since we don't have access to either a
    // Server object or a Request object
    protected void log(Object msg) {
	if (msg == null) {
	    return;
	}
	if (!(msg instanceof String)) {
	    msg = msg.toString();
	}
	Logger.out.println(msg.toString());
    }
}
