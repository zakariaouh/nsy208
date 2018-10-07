/*
 * Connection.java
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
 * Contributor(s): cstevens, suhler.
 *
 * Version:  1.17
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/04/08 15:58:58
 */

package sunlabs.brazil.server;

import java.net.Socket;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Internal "helper" class to manage http connections.
 * Create a thread that lives for the duration of the client socket and handles
 * multiple HTTP requests.  Packages each HTTP request from the socket into
 * an HttpRequest object and passes it to the <code>respond</code> method of
 * the server's main HttpHandler.  If an error occurs while handling a
 * request, the socket is closed and this thread ends.
 *
 * @author	Colin Stevens
 * @version	1.17, 04/08/02
 */
class Connection implements Runnable
{
    /**
     * The Server that created this handler.
     */
    Server server;

    /**
     * The client socket.
     */
    Socket sock;
    
    /**
     * The current request state
     */
    Request request;

    /**
     * Constructs a new Connection and starts it running.
     */
    Connection(Server server, Socket sock)
    {
	this.server = server;
	this.sock = sock;

	request = new Request(server, sock);
    }

    /**
     * Loop reading HTTP requests from the socket until there is an error,
     * the client requests that the socket be closed, or the client exceeds
     * the maximum number of requests allowed on a single socket.
     */
    public void
    run()
    {
	try 
	{
	    sock.setSoTimeout(server.timeout);

	    while (request.shouldKeepAlive()) 
	    {
	    	if (request.getRequest() == false) 
	    	{
	    		break;
	    	}
	    	
	    	server.requestCount++;
	    	
	    	if (server.handler.respond(request) == false) 
	    	{
	    		request.sendError(404, null, request.url);
	    	}
	    	
	    	request.out.flush();
	    	
	    	server.log(Server.LOG_LOG, null, "request done");
	    }
	} 
	catch (InterruptedIOException e) {
	    /*
	     * A read timed out, or (rarely) this thread was interrupted.
	     *
	     * Thread.interrupt() generates an InterruptedIOException that
	     * cannot be 100% discriminated from an InterruptedIOException
	     * caused by a read timeout.
	     *
	     * Under jdk-1.1, a Thread.interrupt() call generates an
	     * InterruptedIOException with the detail message
	     * "operation interrupted".
	     *
	     * Under jdk-1.2, a Thread.interrupt() call generates an
	     * InterruptedIOException with the detail message
	     * "Interrupted system call".
	     *
	     * In order to make the automated test scripts easier to write
	     * and run under both jdk-1.2 and jdk-1.1, suppress the varying
	     * InterruptedIOException log messages due to Thread.interrupt(),
	     * which only happens when the server is being shut down by
	     * the test script anyhow.
	     */

	    String msg = e.getMessage();
	    if ((msg == null) || (msg.indexOf("terrupted") < 0)) { 
		request.sendError(408, msg, null);
	    }
	} catch (IOException e) {
	    /*
	     * Expected exception, due to not being able to write back to
	     * client, etc.
	     */
	    server.log(Server.LOG_INFORMATIONAL, e.getMessage(),
		    "I/O error on socket");
	} catch (Exception e) {
	    /* 
	     * Unexpected exception.
	     */

	    e.printStackTrace();
	    request.sendError(500, e.toString(), "unexpected error");
	} finally {
	    server.log(Server.LOG_INFORMATIONAL, null, "socket close");
	    try {
	    	request.out.flush();
	    } catch (IOException e) {}
	    try {
		sock.close();
	    } catch (IOException e) {}
	}
    }
}
