/*
 * Server.java
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
 * Contributor(s): cstevens, drach, rinaldo, suhler.
 *
 * Version:  1.46
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/02/12 09:17:47
 */

package sunlabs.brazil.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import nesmid.util.Logger;
import sunlabs.brazil.properties.PropertiesList;

/**
 * Yet another HTTP/1.1 server.
 * This class is the core of a light weight Web Server.  This server
 * is started as a Thread listening on the supplied port, and
 * dispatches to an implementation of
 * a {@link Handler} to service http requests.  If no handler is
 * supplied, then the {@link FileHandler} is used.
 * A {@link ChainHandler} is provided to allow multiple handlers in one server.
 * <p>
 * Limitations:
 * <ul>
 * <li>Starts a new thread for each connection.  This may be expensive.  
 * </ul>
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	1.46, 02/02/12
 */

public class Server 
    extends Thread 
{
    /**
     * The listening socket.  Every time a new socket is accepted,
     * a new thread is created to read the HTTP requests from it.  
     */
    public ServerSocket listen;

    /**
     * The main Handler whose <code>respond</code> method is called for
     * every HTTP request.  The <code>respond</code> method must be
     * thread-safe since it handles HTTP requests concurrently from all the
     * accepted sockets.
     *
     * @see	Handler#respond
     */
    private String handlerName;
    public Handler handler;

    /**
     * Hashtable containing arbitrary information that may be of interest to
     * a Handler.  This table is available to both methods of the
     * {@link Handler} interface, as {@link Server#props} in the
     * {@link Handler#init(Server, String)}
     * method, and as the default properties of
     * {@link Request#props} in the {@link Handler#respond(Request)}
     * method.
     */
    
    public Properties props = null;

    /**
     * The hostname that this Server should use to identify itself in
     * an HTTP Redirect.  If <code>null</code>, the hostname is derived
     * by calling <code>InetAddress.getHostAddress</code>.
     * <p>
     * <code>InetAddress.getHostName</code> would generally be the wrong
     * thing to return because it returns only the base machine name
     * <code>xxx</code> and not the machine name as it needs to appear
     * to the rest of the network, such as <code>xxx.yyy.com</code>.
     * <p>
     * The default value is <code>null</code>.
     */

    public String hostName = null;

    /**
     * The protocol used to access this resource.  Normally <code>http</code>, but
     * can be changed for <code>ssl</code> to <code>https</code>
     */

    public String protocol = "http";

    /**
     * If non-null, restrict connections to just the specified ip addresses.
     * <p>
     * The default value is <code>null</code>.
     */
    public InetAddress[] restrict = null;

    /**
     * The string to return as the value for the "Server:" line in the HTTP
     * response header.  If <code>null</code>, then no "Server:" line is
     * returned.
     */
    public String name = "Brazil/2.0";

    /**
     * The handler is passed a prefix to identify which items in the
     * properties object are relevent.  By convention, non-empty strings
     * end with ".", allowing nested prefixes to be easily distinguished.
     */

    public String prefix = "";

    /**
     * Time in milliseconds before this Server closes an idle socket or
     * in-progress request.
     * <p>
     * The default value is <code>30000</code>.
     */
    public int timeout = 30000;

    /**
     * Maximum number of consecutive requests allowed on a single
     * kept-alive socket.
     * <p>
     * The default value is <code>25</code>.
     */
    public int maxRequests = 25;

    /**
     * The max number of threads allowed for the entire VM
     * (default is 250).
     */
    public int maxThreads = 15; // TINI maxThreads = 250;

    /**
     * Maximum amout of POST data allowed per request (in bytes)
     * (default = 2Meg).
     */
    public int maxPost=8182; // TINI maxPost = 2097152;		// 2 Meg

    /**
     * Default buffer size for copies to and from client sockets.  
     * (default is 8192)
     */
    public int bufsize = 8192;
    
    /**
     * Count of accepted connections so far.
     */
    public int acceptCount = 0;
    
    /**
     * Count of HTTP requests received so far.
     */
    public int requestCount = 0;

    /**
     * Count of errors that occurred so far.
     */
    public int errorCount = 0;

    /**
     * The diagnostic level. 0->least, 5->most
     */

    public int logLevel = LOG_LOG;

    /**
     * If set, the server will terminate with an initialization failure
     * just before creating the listen socket.
     */

    public boolean initFailure = false;

    ThreadGroup group;

    /**
     * Create a server using the provided listener socket.  
     * <p>
     * This server will call the <code>Handler.respond</code> method
     * of the specified handler.  The specified handler should either
     * respond to the request or perform further dispatches to other
     * handlers.
     *
     * @param	listen
     *		The socket this server should listen to.
     *		For ordinary sockets, this is simply: <code>
     *		new ServerSocket(port)</code>, where <code>port</code>
     *		is the network port to listen on.  Alternate implementations
     *		of <code>ServerSocket</code>, such as <b>ssl</b> versions
     *		may be used instead.
     * @param   handlerName
     *		The name of the handler used to process http requests.
     *		It must implement the {@link Handler} interface.
     * @param	props
     *		Arbitrary information made available to the handler.
     *		May be <code>null</code>.
     *
     * @see	FileHandler
     * @see	ChainHandler
     */

    public
    Server(ServerSocket listen, String handlerName, Properties props)
    {
	setup(listen, handlerName, props);
    }

    /**
     * Set up the server.  this allows a server to be created with 
     * newInstance() followed by setup(), instead of using the
     * above initializer, making it easier to start sub-classes
     * of the server.
     */
    public Server() {}

    public boolean
    setup(ServerSocket listen, String handlerName, Properties props)
    {
	if (this.props != null) {
	    return false;	// alreasdy initialized
	}
	if (props == null) {
	    props = new Properties();
	}
	this.listen = listen;
	this.handlerName = handlerName;
	this.props=props;
	if (props.get("debugProps") != null) {
	    PropertiesList.debug = true;
	}
	return true;
    }

    public boolean
    init() {
	if (props == null) {
	    log(LOG_ERROR, "server", "Not properly initialized!");
	    return false;
	}
        group = new ThreadGroup(prefix);
	if (hostName == null) {
	    try {
	       hostName = InetAddress.getLocalHost().getHostAddress();
	    } catch (UnknownHostException e) {
	       log(LOG_ERROR, "server",
	           "Can't find my own name, using \"localhost\"" +
		   " (redirects may not work)");
	       hostName="localhost";
	    }

	}
	if (Thread.currentThread().getName().startsWith("Thread-")) {
	    Thread.currentThread().setName("server");
	}

	handler = ChainHandler.initHandler(this, prefix, handlerName);

	if (handler == null) {
	    return false;
	}
	if (initFailure) {
	    log(LOG_ERROR, handlerName, "Initilization failure");
	    return false;
	}
	return true;
    }

    /**
     * Loops, accepting socket connections and replying to HTTP requests.
     * This is called indirectly via Thread.start().
     * <p>
     * Many things in the server are not initialized until this point,
     * because the user may have set some related configuration options
     * between the time this server was allocated and the time it was
     * started.  For instance, the main <code>Handler</code> is not
     * initialized until now, because its <code>Handler.init</code> method
     * may have wanted to examine server member variables such as
     * <code>hostName</code> or <code>bufsize</code>.
     */
    public void
    run()
    {
	try {
	    if (init() == false) {
		return;
	    }

	    listen.setSoTimeout(0);
	    while (true) 
	    {
		/*
		 * Blocks until we have a connection on the socket.
		 */
		Socket sock = listen.accept();
		
		String threadName = sock.getInetAddress().getHostAddress();
		
		log(LOG_INFORMATIONAL, threadName, "new connection");

		allowed:
		if (restrict != null) 
		{
		    InetAddress addr = sock.getInetAddress();
		    for (int i = 0; i < restrict.length; i++) 
		    {
		    	if (restrict[i].equals(addr)) {
		    		break allowed;
		    	}
		    }
		    log(LOG_DIAGNOSTIC, addr, "rejected request");
		    sock.close();
		    continue;
		}

		// A pseudo-busy loop!!!

		boolean warn=false;
		
		while (Thread.activeCount() > maxThreads) 
		{
		    if (!warn) 
		    {
			log(LOG_WARNING, sock, 
				"Too many threads: " + acceptCount);
		    }
		    Thread.yield();
		    warn = true;
		}
	
		new Thread(group, new Connection(this, sock),
			threadName + "-" + acceptCount).start();
		acceptCount++;
	    }
	} catch (IOException e) {
	    /*
	     * Quit anyhow.
	     */
	} finally {
	    try {
		listen.close();

		Thread[] sub = new Thread[15]; // TINI
		int count;
		while ((count = group.enumerate(sub, true)) > 0) {
		    for (int i = 0; i < count; i++) {
			sub[i].interrupt();
			sub[i].join();
		    }
		    yield();
		}
	    } catch (Exception e) {}

	    group = null;
	}
    }

    /**
     * Stop the server, and kill all pending requests
     */
    public void
    close()
    {
	try {
	    this.interrupt();
	    this.join();
	} catch (Exception e) {}

	log(LOG_WARNING, null, "server stopped");
    }

    public static final int LOG_ERROR=1;		// most severe
    public static final int LOG_WARNING=2;
    public static final int LOG_LOG=3;
    public static final int LOG_INFORMATIONAL=4;
    public static final int LOG_DIAGNOSTIC=5;	// least useful

    /**
     * Logs information about the socket to <code>Logger.out</code>.  
     *
     * @param	level	    Controls the verbosity (0=least 5=most)
     * @param	obj	    The object that the message relates to.
     * @param	message	    The message to be logged.
     */

    public void 
    log(int level, Object obj, String message)
    {
	if (level <= logLevel) {
	    Logger.out.println("LOG: " + level + " " + prefix
		    + listen.getLocalPort() + "-"
		    + Thread.currentThread().getName() + ": ");
	    if (obj != null) {
		Logger.out.println(obj.toString());
		Logger.out.println(": ");
	    }
	    Logger.out.println(message);
	}
    }
}
