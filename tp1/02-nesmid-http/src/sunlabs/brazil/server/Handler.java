/*
 * Handler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 1998-2001 Sun Microsystems, Inc.
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
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, suhler.
 *
 * Version:  1.8
 * Created by suhler on 98/09/14
 * Last modified by suhler on 01/08/21 10:58:46
 */

package sunlabs.brazil.server;

import java.io.IOException;

/**
 * The interface for writing HTTP handlers.  Provides basic functionality
 * to accept HTTP requests and dispatch to methods that handle the request.
 * <p>
 * The {@link #init(Server, String)} method is called before this
 * <code>Handler</code> processes the first HTTP request, to allow it to
 * prepare itself, such as by allocating any resources needed for the
 * lifetime of the <code>server</code>.
 * <p>
 * The {@link #respond(Request)} method is called to handle an HTTP request.
 * This method, and all methods it calls must be thread-safe since they may
 * handle HTTP requests from multiple sockets concurrently.  However, each
 * concurrent request gets its own individual {@link Request} object.
 * <p>
 * Any instance variables should be initialized in the
 * {@link #init(Server, String)}, and only referenced, but not set in the
 * {@link #respond(Request)} method.  If any state needs to be retained, 
 * it should be done either by associating it with the {@link Request}
 * object, or using the
 * {@link sunlabs.brazil.session.SessionManager session manager}.
 * Class statics should be avoided, as it is possible, and even common to
 * run multiple unrelated Brazil servers in the same JVM.  As above, the
 * {@link sunlabs.brazil.session.SessionManager session manager}
 * should be used instead.
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	1.8, 01/08/21
 */

public interface Handler {
    /**
     * Initializes the handler.
     *
     * @param	server
     *		The HTTP server that created this <code>Handler</code>.
     *		Typical <code>Handler</code>s will use {@link Server#props}
     *		to obtain run-time configuration information.
     *
     * @param	prefix
     *		A prefix that this <code>Handler</code> may prepend to all
     *		of the keys that it uses to extract configuration information
     *		from {@link Server#props}.  This is set (by the {@link Server}
     *		and {@link ChainHandler}) to help avoid configuration parameter
     *		namespace collisions.
     *		<p>
     *		For example, if a <code>Handler</code> uses the property
     *		"account", and the specified prefix is "bank.", then the
     *		<code>Handler</code> should actually examine the property
     *		"bank.account" in <code>Server.props</code>.
     *
     * @return	<code>true</code> if this <code>Handler</code> initialized
     *		successfully, <code>false</code> otherwise.  If
     *		<code>false</code> is returned, this <code>Handler</code>
     *		should not be used.
     */
    boolean init(Server server, String prefix);

    /**
     * Responds to an HTTP request.
     *
     * @param	request
     *		The <code>Request</code> object that represents the HTTP
     *		request.
     *
     * @return	<code>true</code> if the request was handled.  A request was
     *		handled if a response was supplied to the client, typically
     *		by calling <code>Request.sendResponse()</code> or
     *		<code>Request.sendError</code>.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client.  Typically, in that case, the <code>Server</code>
     *		will (try to) send an error message to the client and then
     *		close the client's connection.
     *		<p>
     *		The <code>IOException</code> should not be used to silently
     *		ignore problems such as being unable to access some
     *		server-side resource (for example getting a
     *		<code>FileNotFoundException</code> due to not being able
     *		to open a file).  In that case, the <code>Handler</code>'s
     *		duty is to turn that <code>IOException</code> into a
     *		HTTP response indicating, in this case, that a file could
     *		not be found.
     */
    boolean respond(Request request) throws IOException;
}
