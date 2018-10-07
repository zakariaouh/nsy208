/*
 * ChainHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 1998-2002 Sun Microsystems, Inc.
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
 * Contributor(s): cstevens, rinaldo, suhler.
 *
 * Version:  1.29
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/06/13 18:26:54
 */

package sunlabs.brazil.server;

import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import nesmid.util.Logger;

/**
 * Allows multiple handlers to be invoked sequentially for a single HTTP
 * request. A list of handlers is supplied when this <code>ChainHandler</code>
 * is initialized. When an HTTP request is received by this
 * <code>ChainHandler</code>, each of the handlers from the list is called in
 * turn until one of them responds and returns <code>true</code>.
 * <p>
 * A useful trick is that some handlers can be run by a
 * <code>ChainHandler</code> for their side effects. The handler can modify
 * the <code>Request</code> object and then return <code>false</code>; the
 * next handler in the list will get a crack at the modified request.
 * <p>
 * The following configuration parameters eare used to initialize this
 * <code>Handler</code>:
 * <dl class=props>
 * 
 * <dt> <code>handlers</code>
 * <dd> A list of <code>Handler</code> names that will be invoked in the given
 * order to handle the request. These are considered the "wrapped" handlers.
 * These handlers will all be initialized at startup by {@link #init}. For each
 * name in the list, the property <code><i>name</i>.class</code> is examined
 * to determine which class to use for this handler. Then <code>name</code> is
 * used as the prefix in the handler's init() method.
 * <dt> <code>report</code>
 * <dd> If set, this property will be set to the name of the handler that
 * handled the request (e.g. returned true).
 * <dt> <code>exitOnError</code>
 * <dd> If set, the server's <code>initFailure</code> will set any of the
 * handlers fail to initialize. No handler prefix is required.
 * <dt> <code>prefix</code>
 * <dd> If set, All url's must start with this, or the request will be ignored
 * </dl>
 * 
 * @see Handler
 * 
 * @author Stephen Uhler (stephen.uhler@sun.com)
 * @author Colin Stevens (colin.stevens@sun.com)
 * @version 1.29, 02/06/13
 */
public class ChainHandler implements Handler {
	private static final String HANDLERS = "handlers";

	private static final String EXIT_ON_ERROR = "exitOnError";

	/**
	 * The array of handlers that will be invoked to handle the request.
	 */
	public Handler[] handlers;

	/**
	 * The names of the above <code>handlers</code> as specified by the
	 * configuration parameters. Used for logging the names of each
	 * <code>Handler</code> as it is invoked.
	 */
	public String[] names;

	/**
	 * The prefix used to initialize this <code>ChainHandler</code>, used for
	 * logging.
	 */
	public String prefix;

	/**
	 * The URL prefix that must match for this handler to run
	 */
	public String urlPrefix;

	/**
	 * The name (if any) of the property to receive the name of the handler that
	 * handled the request.
	 */
	public String report;

	/**
	 * A flag to require the successfull initialization of all handlers.
	 */
	public boolean exitOnError = false;

	/**
	 * Initializes this <code>ChainHandler</code> by initializing all the
	 * "wrapped" handlers in the list of handlers. If a wrapped handler cannot
	 * be initialized, this method logs a message and skips it. If no handlers
	 * were specified, or no handlers were successfully initialized, then the
	 * initialization of this <code>ChainHandler</code> is considered to have
	 * failed.
	 * 
	 * @param server
	 *            The HTTP server that created this <code>ChainHandler</code>.
	 * 
	 * @param prefix
	 *            The prefix for this <code>ChainHandler</code>'s properties.
	 * 
	 * @return <code>true</code> if at least one of the wrapped handlers was
	 *         successfully initialized.
	 */
	public boolean init(Server server, String prefix) {

		try {
			this.prefix = prefix;

			Properties props = server.props;

			exitOnError = (props.getProperty(prefix + EXIT_ON_ERROR, (props
					.getProperty(EXIT_ON_ERROR))) != null);

			String str = props.getProperty(prefix + HANDLERS, "");

			report = props.getProperty(prefix + "report");

			urlPrefix = props.getProperty(prefix + "prefix");

			StringTokenizer names = new StringTokenizer(str);

			Vector handlerVec = new Vector();
			Vector nameVec = new Vector();

			while (names.hasMoreTokens()) {
				String name = names.nextToken();
				server.log(Server.LOG_DIAGNOSTIC, prefix, "starting handler: "
						+ name);

				Handler h = initHandler(server, prefix, name);
				if (h != null) {
					handlerVec.addElement(h);
					nameVec.addElement(name);
				} else if (exitOnError) {
					server.initFailure = true;
					System.err.println("Handler initialization failure in ("
							+ prefix + ") for handler: " + name);
				}
			}
			if (handlerVec.size() == 0) {
				server.log(Server.LOG_DIAGNOSTIC, prefix, "no handlers");
				return false;
			}

			this.handlers = new Handler[handlerVec.size()];
			handlerVec.copyInto(this.handlers);

			this.names = new String[nameVec.size()];
			nameVec.copyInto(this.names);
			System.out.println(" Succed @ Server.init ");
			return true;
		} catch (Exception e) {
			System.out.println(" Error @ Server.init " + e);
			return false;
		}
	}

	/**
	 * Helper function that allocates and initializes a new <code>Handler</code>,
	 * given its name. In addition to the <code>ChainHandler</code>, several
	 * other handlers contain embedded <code>Handler</code>s -- this method
	 * can be used to initialize those embedded <code>Handler</code>s.
	 * <p>
	 * If there is an error initializing the specified <code>Handler</code>,
	 * this method will log a dignostic message to the server and return
	 * <code>null</code>. This happens if the specified class cannot be found
	 * or instantiated, if the specified class is not actually a
	 * <code>Handler</code>, if the <code>Handler.init</code> method
	 * returns <code>false</code>, or if there is any other exception.
	 * 
	 * @param server
	 *            The server that will own the new <code>Handler</code>.
	 *            Mainly used for the server's properties, which contain the
	 *            configuration parameters for the new handler.
	 * 
	 * @param prefix
	 *            The prefix in the server's properties for the new
	 *            <code>Handler</code>'s configuration parameters. The prefix
	 *            is prepended to the configuation parameters used by the
	 *            <code>Handler</code>.
	 * 
	 * @param name
	 *            The name of the new <code>Handler</code>. The name can be
	 *            one of two forms:
	 *            <ol>
	 * 
	 * <li> The name of the Java class for the <code>Handler</code>. This
	 * <code>Handler</code> will be initialized using the <code>prefix</code>
	 * specified above.
	 * 
	 * <li> A symbolic <code>name</code>. The configuration parameter
	 * <code><i>name</i>.class</code> is the name of the Java class for the
	 * <code>Handler</code>. The above <code>prefix</code> will be ignored
	 * and this <code>Handler</code> will be initialized with the prefix "<code><i>name</i>.</code>"
	 * (the symbolic name followed by a ".").
	 * </ol>
	 * 
	 * @return The newly allocated <code>Handler</code>, or <code>null</code>
	 *         if the <code>Handler</code> could not be allocated.
	 */
	public static Handler initHandler(Server server, String prefix, String name) {
		String className = server.props.getProperty(name + ".class");
		if (className == null) {
			className = name;
		} else {
			prefix = null;
		}
		if (prefix == null) {
			prefix = name + ".";
		}

		try {
			Handler h = (Handler) Class.forName(className.trim()).newInstance();

			

			if (prefix.equals("thing"))
				Logger.out.println("" + h.init(server, prefix));

			if (h.init(server, prefix)) {
				return h;
			}
			server.log(Server.LOG_WARNING, name, "handler did not initialize");
		} catch (ClassNotFoundException e) {
			server.log(Server.LOG_WARNING, className, "no such class");
		} catch (IllegalArgumentException e) {
			server.log(Server.LOG_WARNING, className,
					"Invalid argument during instantiation");
		} catch (ClassCastException e) {
			server.log(Server.LOG_WARNING, className, "is not a Handler");
		} catch (Exception e) {
			// e.printStackTrace(Logger.out);
			server
					.log(Server.LOG_WARNING, name,
							"CA VA PAS error initializing");
			// e.printStackTrace();
		}
		return null;
	}

	/**
	 * Calls each of the <code>Handler</code>s in turn until one of them
	 * returns <code>true</code>.
	 * 
	 * @param request
	 *            The HTTP request.
	 * 
	 * @return <code>true</code> if one of the <code>Handler</code>s
	 *         returns <code>true</code>, <code>false</code> otherwise.
	 * 
	 * @throws IOException
	 *             if one of the <code>Handler</code>s throws an
	 *             <code>IOException</code> while responding.
	 */
	public boolean respond(Request request) throws IOException {
		if (urlPrefix != null && !request.url.startsWith(urlPrefix)) {
			return false;
		}
		for (int i = 0; i < handlers.length; i++) {
			request.log(Server.LOG_DIAGNOSTIC, prefix, "invoking handler: "
					+ names[i]);

			if (handlers[i].respond(request)) {
				if (report != null) {
					request.props.put(report, names[i]);
				}
				return true;
			}
		}
		return false;
	}
}
