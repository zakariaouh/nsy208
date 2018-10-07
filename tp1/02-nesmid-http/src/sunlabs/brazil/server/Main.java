/*
 * Main.java
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
 * Contributor(s): cstevens, drach, suhler.
 *
 * Version:  1.30
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/07/24 10:47:48
 */

package sunlabs.brazil.server;

/**
 * Start an HTTP/1.1 server.  The port number and handler class
 * are provided as arguments.
 *
 * @author	Stephen Uhler
 * @author	Colin Stevens
 * @version	1.30, 07/24/02
 */

//import sunlabs.brazil.util.Format;
import nesmid.util.Logger;
import nesmid.util.RunnableWithContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * Sample <b>main</b> program for starting an http server.
 *
 * A new thread is started for each
 * {@link Server},
 * listening on a socket for HTTP connections.
 * As each connection is accepted, a
 * {@link Request} object is constructed,
 * and the registered
 * {@link Handler} is called.
 * The configuration properties required by the server
 * and the handler (or handlers),  are gathered
 * from command line arguments and configuration files specified on the
 * command line. 
 * <p>
 * The command line arguments are processed in order from left to
 * right, with the results being accumulated in a properties object.
 * The server is then started with {@link Server#props} set to the
 * final value of the properties.
 * Some of the properties are interpreted directly by the server, 
 * such as the port to listen on, or the handler to use
 * (see {@link Server} for the complete list).  The rest
 * are arbitrary name/value pairs that may be used by the handler.
 * <p>
 * Although any of the options may be specified as name/value pairs, 
 * some of them: the ones interpreted by the server, the default
 * handler ({@link FileHandler}, or {@link Main}, 
 * may be prefixed with a "-".
 * Those options are explained below:
 * <dl>
 * <dt>  -p(ort)    <dd>The network port number to run the server on (defaults to 8080)
 * <dt>  -r(oot)    <dd>The document root directory, used by the FileHandler (defaults to .)
 * <dt>  -h(andler) <dd>The document handler class
 *			   (defaults to {@link FileHandler sunlabs.brazil.handler.FileHandler})
 * <dt>  -c(onfig)  <dd>A java properties file to add to the current properties.
 *			There may be several <i>-config</i> options.  Each
 *			file is added to the current properties.
 *			If the properties file contains a <code>root</code>
 *			property, it is treated specially.  See below.
 *			If the config file is not found in the filesystem, 
 *			it is read from the jar file, with this class as the
 *			virtual current directory if a relative path
 *			is provided.
 * <dt>  -i(p)      <dd>A space seperated list of hosts allowed to access this server
 *			If none are supplied, any host may connect.  The ip addresses
 *			are resolved once, at startup time.
 * <dt>  -l(og)     <dd>The log level (0->none, 5->max)
 *			Causes diagnostic output on the standard output.
 * <dt>  -s(tart)   <dd>Start a server.
 *			Allows multiple servers to be started at once.
 *			As soon as a <i>-s</i> is processed, as server is
 *			started as if all the options had been processed,
 *			then the current properties are cleared.
 *			Any options that follow are used for the next server.
 * <dt>  -S(ubstitute)  <dd>Perform ${..} substitutions on the current
 *			values.
 * </dl>
 * <p>
 * Following these options, any additional additional pairs of 
 * names and values (no "-"'s allowed) are placed directly in
 * {@link Server#props}.
 * <p>
 * If the resource "/sunlabs/brazil/server/config" is found, it is used
 * to initialize the configuration.
 * <p>
 * If a non absolute <code>root</code> property is specified in a
 * <i>configuration</i> file, it is modified to resolve relative to the
 * directory containing the <i>configuration</i> file, and not the directory
 * in which the server was started.  If multiple <i>configuration</i> files
 * with root properties (or <code>-r</code> options, or "root" properties)
 * are specified, the last one tekes precedence.
 * <p>
 * The "serverClass" property may be set to the string to use as the server's
 * class, instead of "sunlabs.brazil.server.Server"
 */

public class Main 
{
    static final String CONFIG = "/sunlabs/brazil/server/config";
    static final String LOGGER= "nesmid.util.Func";
    

    public static void main(String[] args) throws Exception
    {
    	
		boolean started = false;
		Properties config = new Properties();
		initProps(config);
		try 
		{
			if(config.getProperty("_LOGGER")==null)
				nesmid.util.Logger.init(LOGGER);
			else
				nesmid.util.Logger.init(""+config.getProperty("_LOGGER"));
		} 
		catch (Exception e) 
		{
			System.out.println(e);
		}
		
		Logger.out.println("Starting the server....");
	

		/*
		 * Try to initialize the server from a resource in the
		 * jar file, if available
		 * TINI
	
		{
		    InputStream in = Main.class.getResourceAsStream(CONFIG);
		    if (in != null) {
		       config.load(in);
		       Logger.out.println("Found default config file");
		       in.close();
		    }
		}
     	TINI */

		int i=0;
		try {
		    String rootBase = null;
		    String root = null;
	
		    for (i = 0; i < args.length; i++) {
		    	started = false;
			if (args[i].startsWith("-he")) {
			    throw new Exception();  // go to usage.
			} else if (args[i].startsWith("-ha")) {
			    config.put("handler",args[++i]);
			} else if (args[i].startsWith("-r")) {
			    root = args[++i];
			    rootBase = null;
			    config.put(FileHandler.ROOT, root);
			} else if (args[i].startsWith("-ho")) {
			    config.put("host",args[++i]);
			} else if (args[i].startsWith("-de")) {
			    config.put("default",args[++i]);
			} else if (args[i].startsWith("-ip")) {
			    config.put("restrict",config.getProperty("restrict","") + " " +
				    InetAddress.getByName(args[++i]));
			} else if (args[i].startsWith("-c")) {
			    String oldRoot = config.getProperty(FileHandler.ROOT);
			    File f = new File(args[++i]);

		    /*
		     * Look for config file in filesystem.  If found, slurp
		     * it in, and adjust the "root" if needed".  Otherwise, 
		     * look for it in the jar file (and leave the root alone).
		     */

		    if (f.canRead()) {
			try {
			    FileInputStream in = new FileInputStream(f);
			    config.load(in);
			    in.close();
			} catch (Exception e) {
			    Logger.out.println("Warning: " + e);
			    continue;
			}
			String newRoot = config.getProperty(FileHandler.ROOT);
			if (newRoot != oldRoot) {
			    root = newRoot;
			    rootBase = f.getPath();
			}
			} /** else { TINI
			InputStream in =Main.class.getResourceAsStream(args[i]);
			if (in != null) {
			   config.load(in);
			   rootBase = null;
			   in.close();
			}
		    }**/
		} else if (args[i].startsWith("-p")) {
		    config.put("port",args[++i]);
		}/*** else if (args[i].startsWith("-S")) { //TINI
		    Enumeration enum = config.propertyNames();
		    while(enum.hasMoreElements()) {
			String key = (String) enum.nextElement();
			config.put(key, 
				Format.subst(config, config.getProperty(key)));
		    }
		} **/ else if (args[i].startsWith("-l")) {
		    config.put("log",args[++i]);
		} else if (args[i].startsWith("-s")) {
		    if (startServer(config)) 
		    {
			Logger.out.println("Server started on " +
				config.getProperty("port", "8080"));
		    }

		    // make sure servers do not share the same properties

		    config = new Properties();
		    initProps(config);
		    started = true;
		} 
		else if (args[i].equals(FileHandler.ROOT)) 
		{
		    root = args[++i];
		    rootBase = null;
		    config.put(FileHandler.ROOT, root);
		} 
		else if (!args[i].startsWith("-")) 
		{
		    config.put(args[i],args[++i]);
		} else {
		    Logger.out.println("Invalid flag : " + args[i]);
		    throw new Exception(); // go to usage.
		}
	    }

	    /*
	     * The last thing that specified a root wins.
	     *
     	     * If the root was last specified on the command line, use that
	     * as the base root of the system.
	     *
	     * If the last thing that specified a root was a config file,
	     * then the root in the config file must be combined with the
	     * basename of that config file to produce the real root.
	     *
	     * If the root wasn't specified at all by anything, then that
	     * is equivalent to leaving the root as the current dir.
	     */

	    if ((rootBase != null) && (new File(root).isAbsolute() == false)) {
                rootBase = rootBase.replace('/', File.separatorChar);
                rootBase = new File(rootBase).getParent();
                if (rootBase != null) {
		    config.put(FileHandler.ROOT,
                            rootBase + File.separator + root);
		}
	    }
	} catch (ArrayIndexOutOfBoundsException e) {
	    Logger.out.println("Missing argument after: " + args[i-1]);
	    return;
	} catch (Exception e) {
	    e.printStackTrace();
	    Logger.out.println("Usage: Main -conf <file> -port <port> " +
		    "-handler <class name> -root <doc_root> -ip <host> " +
		    "<name value>...");
	    return;
	}
		if (!started && startServer(config))
		{
		    Logger.out.println("Server started on " +
			    config.getProperty("port", "8080"));
		}
		if((""+config.getProperty("_RUNNING_PROTOCOLS")).indexOf("bluetooth") >-1)
			startBSTTPServer(config);
    }
    
    public static void  startBSTTPServer(Properties config)
    {	
    	try
    	{	
    		Class cls = Class.forName(""+config.get("bluetooth.main"));
			
    		RunnableWithContext run = (RunnableWithContext) cls.newInstance();
			
			run.props = config;
	    	
			run.props = config;
	    	
	    	if(run.init())
	    	{
	    		Thread t =new Thread(run);
	    		
	    		t.start();
	    	}
    	}
    	catch(Exception e)
    	{
    		Logger.out.println("Unable to start bluetooth server. Please check the configuration file." + e.toString());
    	}
    }

    /**
     * Start a server using the supplied properties.  The following
     * entries are treated.  Specially:
     * <dl>
     * <dt> handler
     * <dd> The name of the handler class (defaults to file handler)
     * <dt> host
     * <dd> The host name for this server
     * <dt> log
     * <dd> Diagnostic output level 0-5 (5=most output)
     * <dt> maxRequests
     * <dd> max number of requests for a single socket (default 25)
     *	    when using persistent connections.
     * <dt> listenQueue
     * <dd> max size of the OS'slisten queue for server sockets
     * <dt> maxPost
     * <dd> max size of a content-length for a Post or Put in bytes.
     *	    (defaults to 2Meg)
     * <dt> maxThreads
     * <dd> max number of threads allowed (defaults to 250)
     * <dt> port
     * <dd> Server port (default 8080)
     * <dt> defaultPrefix
     * <dd> prefix into the properties file, normally the empty string "".
     * <dt> restrict
     * <dd> list of hosts allowed to connect (defaults to no restriction)
     * <dt> timeout
     * <dd> The maximum time to wait for a client to send a complete request.
     *	    Defaults to 30 seconds.
     * <dt> interfaceHost
     * <dd> If specified, a host name that represents the network to server.
     *	    This is for hosts with multiple ip addresses.  If no network
     *	    host is specified, then connections for all interfaces are
     *	    accepted
     * </dl>
     * @param config	The configuration properties for the server
     */
    
    public static boolean
    startServer(Properties config)
    {    	   	
		String handler = FileHandler.class.getName();
		int port = 8080;
		int queue = 10; // TINI 1024;
		
		// is the method invoked from a servlet?
		boolean servlet = config.getProperty("servlet_name") != null;
	
		handler = config.getProperty("handler", handler);
		
		try {
		    String str = config.getProperty("port");
		    port = Integer.decode(str).intValue();
		} catch (Exception e) {}
		try {
		    String str = config.getProperty("listenQueue");
		    queue = Integer.decode(str).intValue();
		} catch (Exception e) {}
	
		Server server = null;
		String interfaceHost =  config.getProperty("interfaceHost");
		String serverClass = config.getProperty("serverClass");
		String errMsg = null;
		try 
		{
	    ServerSocket listen;
	    if (servlet) {  
		listen = (ServerSocket)Class.forName(
                                  "sunlabs.brazil.servlet.BServletServerSocket"
                                  ).newInstance();
	    } else 
		if (interfaceHost != null) {
		listen = new ServerSocket(port, queue,
			InetAddress.getByName(interfaceHost));
	    } else {
		listen = new ServerSocket(port, queue);
	    }
	    if (serverClass != null) {
		server = (Server) Class.forName(serverClass).newInstance();
		server.setup(listen, handler, config);
	    } else {
		server = new Server(listen, handler, config);
	    }
		
		} catch (ClassNotFoundException e) {
		    errMsg = serverClass + " not found for class Server";
		} catch (IllegalAccessException e) {
		    errMsg = serverClass + " not available";
		} catch (InstantiationException e) {
		    errMsg = serverClass + " not instantiatable";
		} catch (ClassCastException e) {
		    errMsg = serverClass + " is not a sub-class of server";
		} catch (UnknownHostException e) {
		    errMsg = interfaceHost +
			    " doesn't represent a known interface";
		} catch (BindException e) {
		    errMsg = "Port " + port + " is already in use";
		} catch (IOException e) {
		    errMsg = "Unable to start server on port " + port +
		    	" :" + e;
		}

		if (errMsg != null) {
		    if (servlet) {
			config.put("_errMsg", errMsg);
		    } else {
			Logger.out.println(errMsg);
		    }
		    return false;
		}

		server.hostName = config.getProperty("host", server.hostName);
		server.prefix = config.getProperty("defaultPrefix", server.prefix);

		try {
		    String str = config.getProperty("maxRequests");
		    server.maxRequests = Integer.decode(str).intValue();
		} catch (Exception e) {}

		try {
		    String str = config.getProperty("maxThreads");
		    server.maxThreads = Integer.decode(str).intValue();
		} catch (Exception e) {}

		try {
		    String str = config.getProperty("maxPost");
		    server.maxPost = Integer.decode(str).intValue();
		} catch (Exception e) {}

		try {
		    String str = config.getProperty("timeout");
		    server.timeout = Integer.decode(str).intValue() * 1000;
		} catch (Exception e) {}

		/*
		 * Turn off keep alives entirely
		 */
		if (config.containsKey("noKeepAlives")) {
		    server.maxRequests = 0;
		}
		try {
		    String str = config.getProperty("log");
		    server.logLevel = Integer.decode(str).intValue();
		} catch (Exception e) {}

	
		{
		    Vector restrict = new Vector();
		    String str = config.getProperty("restrict", "");
		    StringTokenizer st = new StringTokenizer(str);
		    while (st.hasMoreTokens()) {
			try {
			    InetAddress addr = InetAddress.getByName(st.nextToken());
			    restrict.addElement(addr);
			} catch (Exception e) {}
		    }
		    if (restrict.size() > 0) {
			server.restrict = new InetAddress[restrict.size()];
			restrict.copyInto(server.restrict);
		    }
		}

		{
		    String str = config.getProperty("init", "");
		    StringTokenizer st = new StringTokenizer(str);
		    while (st.hasMoreTokens()) {
			String init = st.nextToken();
			server.log(Server.LOG_DIAGNOSTIC, "initializing", init);
	
			Object obj = initObject(server, init);
	
			if (obj == null) {
			    server.log(Server.LOG_DIAGNOSTIC, init,
				    "didn't initialize");
			}
		    }
		}

		if (servlet) 
		{
		    config.put("_server", server);
		} else {
		    server.start();
		}
		//Logger.out.println("") ;
	return true;
    }

    public static Object
    initObject(Server server, String name)
    {
	String className = server.props.getProperty(name + ".class");
	if (className == null) {
	    className = name;
	} 
	String prefix = name + ".";

	Object obj = null;
	try 
	{
		//Logger.out.println("///////// TOT ////////////" +  className);
	    Class type = Class.forName(className);
	    obj = type.newInstance();

	    Class[] types = new Class[] {Server.class, String.class};
	    Object[] args = new Object[] {server, prefix};

	    //Object result = type.getMethod("init", types).invoke(obj, args); TINI
	//	if(type == FileHandler.class)
		  ((Handler)obj).init(server,prefix);
	    //if (Boolean.FALSE.equals(result)) {
		//return null;
	    //}
	    return obj;
	} catch (ClassNotFoundException e) {
	    server.log(Server.LOG_WARNING, className, "no such class");
	} catch (IllegalArgumentException e) {
	    server.log(Server.LOG_WARNING, className, "no such class");
	//} catch (NoSuchMethodException e) {  TINI
	//    return obj;
	} catch (Exception e) 
	{	
		server.log(Server.LOG_WARNING, name, "Error initializing");
		e.printStackTrace();
	}
	return null;
    }

    /**
     * Initialize a properties file with some standard mime types
     * The {@link FileHandler} only delivers files whose suffixes
     * are known to map to mime types.  The server is started with 
     * the suffixes: .html, .txt, .gif, .jpg, .css, .class, and .jar
     * predefined.  If additional types are required, they should be supplied as
     * command line arguments. 
     */

    public static void
    initProps(Properties config) {
	config.put("mime.html",  "text/html");
	config.put("mime.txt",   "text/plain");
	config.put("mime.gif",   "image/gif");
	config.put("mime.jpg",   "image/jpeg");
	config.put("mime.css",   "text/x-css-stylesheet");
	config.put("mime.class", "application/octet-stream");
	config.put("mime.jar",   "application/octet-stream");
	config.put("mime.jib",   "application/octet-stream");
    }
}
