/*
 * Request.java
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
 * Version:  1.71
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/08/21 18:25:46
 */

package sunlabs.brazil.server;
import nesmid.util.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import nesmid.util.HttpUtil;
import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.MimeHeaders;

/**
 * Represents an HTTP transaction.   A new instance is created
 * by the server for each connection.
 * <p>
 * Provides a set of accessor functions to fetch the individual fields
 * of the HTTP request.
 * <p>
 * Utility methods that are generically useful for manipulating HTTP
 * requests are included here as well.  An instance of this class is
 * passed to handlers.  There will be exactly one request object per thead
 * at any time.
 * <p>
 * The fields
 * {@link #headers}, 
 * {@link #query}, and
 * {@link #url}, and the method
 * {@link #getQueryData()}
 * are most often used to examine the content of the request.
 * The field
 * {@link #props}
 * contains information about the server, or up-stream handlers.
 * <p>
 * The methods
 * {@link #sendResponse(String, String, int)} and
 * {@link Request#sendError(int, String)}
 * are commonly used to return content to the client.  The methods
 * {@link #addHeader(String)} and
 * {@link #setStatus(int)} can be used to modify the response headers
 * and return code respectively before the response is sent.
 * <p>
 * Many of the other methods are used internally, but can be useful to
 * handlers that need finer control over the output that the above methods
 * provide.  Note that the order of the methods is important.  For instance,
 * the user cannot change the HTTP response headers (by calling the
 * <code>addHeader</code> method or by modifying the
 * <code>responseHeaders</code> field) after having already sent an HTTP
 * response.
 * <p>
 * A number of the fields in the <code>Request</code> object are public,
 * by design.  Many of the methods are convenience methods; the underlying
 * data fields are meant to be accessed for more complicated operations,
 * such as changing the URL or deleting HTTP response headers.  
 *
 * @see		Handler
 * @see		Server
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	1.71, 02/08/21
 */
public class Request
{
    public Server server;
    public Socket sock;
   
    protected HttpInputStream in;
  

    /**
     * A set of properties local to this request.  The property is wrapped
     * in a <code>PropertiesList</code> object and initially is the head
     * of a linked list of properties that are searched in order.
     * This is useful for handlers that wish to communicate via properties
     * to down-stream handlers, such as modifying a server property for a
     * particular request.  Some handlers may even add entire new sets of
     * properties onto the front of <code>request.props</code> to temporarily
     * modify the properties seen by downstream handlers. 
     */
    public PropertiesList props;

    /**
     * A <code>PropertiesList</code> object that wraps
     * <code>server.props</code>.  When this <code>request</code> is
     * created, a new <code>PropertiesList</code> wrapping 
     * <code>server.props</code> is created and added to a list consisting
     * only of <code>props</code> and <code>serverProps</code>.
     */
    public PropertiesList serverProps;

    /**
     * The HTTP response to the client is written to this stream.  Normally
     * the convenience methods, such as <code>sendResponse</code>, are used
     * to send the response, but this field is available if a handler
     * needs to generate the response specially.
     * <p>
     * If the user chooses to write the response directly to this stream, the
     * user is still encouraged to use the convenience methods, such as
     * <code>sendHeaders</code>, to first send the HTTP response headers.
     * The {@link sunlabs.brazil.filter.FilterHandler}
     * examines the HTTP response headers
     * set by the convenience methods to determine whether to filter the
     * output.  
     * <p>
     * Note that the HTTP response headers will <b>not</b> automatically be
     * sent as a side effect if the user writes to this stream.  The user
     * would either need to call the convenience method
     * <code>sendHeaders</code> or need to generate the HTTP response headers
     * themselves.
     * <p>
     * This variable is declared as a <code>Request.HttpOutputStream</code>,
     * which provides the convenience method <code>writeBytes</code> to write
     * the byte representation of a string back to the client.  If the user
     * does not need this functionality, this variable may be accessed
     * simply as a normal <code>OutputStream</code>.
     *
     * @see	#sendResponse(String, String, int)
     * @see	#sendHeaders(int, String, int)
     */
    public HttpOutputStream out;

    /*
     * How many requests this <code>Request</code> will handle.  If this goes
     * to 0, then the connection will be closed even if
     * <code>keepAlive</code> is true.
     */
    protected int requestsLeft;


    //-----------------------------------------------------------------------

    /**
     * The HTTP request method, such as "GET", "POST", or "PUT".
     */
    public String method;

    /**
     * The URL specified in the request, not including any "?" query
     * string.
     */
    public String url;

    /**
     * The query string specified after the URL, or <code>""</code> if no
     * query string was specified.
     */
    public String query;

    /**
     * The HTTP protocol specified in the request, either "HTTP/1.0" or
     * "HTTP/1.1".
     *
     * @see	#version
     */
    public String protocol;

    /**
     * Derived from {@link #protocol}, the version of the HTTP protocol
     * used for this request.  Either <code>10</code> for "HTTP/1.0" or
     * <code>11</code> for "HTTP/1.1".
     */
    public int version;

    /**
     * The HTTP request headers.  Keys and values in this table correspond
     * the field names and values from each line in the HTTP header;
     * field names are case-insensitive, but the case of the values is
     * preserved.  The order of entries in this table corresponds to the
     * order in which the request headers were seen.  Multiple header lines
     * with the same key are stored as separate entries in the table.
     */
    public MimeHeaders headers;

    /**
     * The uploaded content of this request, usually from a POST.  Set to
     * <code>null</code> if the request has no content.
     */
    public byte[] postData;

    /**
     * <code>true</code> if the client requested a persistent connection,
     * <code>false</code> otherwise.  Derived from the {@link #protocol} and
     * the {@link #headers},
     * <p>
     * When "Keep-Alive" is requested, the client can issue multiple,
     * consecutive requests via a single socket connection.  By default: <ul>
     * <li> HTTP/1.0 requests are not Keep-Alive, unless the
     * "Connection: Keep-Alive" header was present.
     * <li> HTTP/1.1 requests are Keep-Alive, unless the "Connection: close"
     * header was present.
     * </ul>
     * The user can change this value from <code>true</code> to
     * <code>false</code> to forcefully close the connection to the client
     * after sending the response.  The user can change this value from
     * <code>false</code> to <code>true</code> if the client is using a
     * different header to request a persistent connection.  See
     * {@link #connectionHeader}.
     * <p>
     * Regardless of this value, if an error is detected while receiving
     * or responding to an HTTP request, the connection will be closed.
     */
    public boolean keepAlive;

    /**
     * The header "Connection" usually controls whether the client
     * connection will be of type "Keep-Alive" or "close".  The same
     * header is written back to the client in the response headers.
     * <p>
     * The field {@link #keepAlive} is set based on the value of the
     * "Connection" header.  However, not all clients use "Connection"
     * to request that the connection be kept alive.  For instance (although
     * it does not appear in the HTTP/1.0 or HTTP/1.1 documentation) both
     * Netscape and IE use the "Proxy-Connection" header when issuing
     * requests via an HTTP proxy.  If a <code>Handler</code> is written to
     * respond to HTTP proxy requests, it should set <code>keepAlive</code>
     * depending on the value of the "Proxy-Connection" header, and set
     * <code>connectionHeader</code> to "Proxy-Connection", since the
     * convenience methods like <code>setResponse()</code> use these fields
     * when constructing the response.  The server does not handle the
     * "Proxy-Connection" header by default, since trying to pre-anticipate
     * all the exceptions to the specification is a "slippery slope".
     */
    public String connectionHeader;

    //-----------------------------------------------------------------------

    protected int statusCode;
    protected String statusPhrase;

    /**
     * The HTTP response headers.  Keys and values in this table correspond
     * to the HTTP headers that will be written back to the client when
     * the response is sent.  The order of entries in this table corresponds
     * to the order in which the HTTP headers will be sent.  Multiple header
     * lines with the same key will be stored as separate entries in the
     * table.
     *
     * @see	#addHeader(String, String)
     */
    public MimeHeaders responseHeaders;

    /*
     * True if the headers have already been sent, so that if sendError()
     * is called in the middle of sending a response, it won't send the
     * headers again, but will cause the connection to be closed afterwards.
     */
    protected boolean headersSent;

    /**
     * Time stamp for start of this request - set, but not used.
     */
    public long startMillis;

    /**
     * Create a new http request.  Requests are created by the server for
     * use by handlers.
     *
     * @param	server
     *		The server that owns this request.
     *
     * @param	sock
     *		The socket of the incoming HTTP request.
     */
    protected Request(Server server, Socket sock)
    {
    	this.server = server;
    	this.sock = sock;

    	try 
    	{
    		in = new HttpInputStream(
    				new BufferedInputStream(sock.getInputStream()));
    		out = new HttpOutputStream(
    				new BufferedOutputStream(sock.getOutputStream()));
    	} 
    	catch (IOException e) 
    	{
	    /*
	     * Logically we shouldn't get an error obtaining the streams from
	     * the socket, but if it does, it will be caught later as a
	     * NullPointerException by the Connection.run() method the first
	     * time we attempt to read from the socket.
	     */
    	}
	
    	requestsLeft = server.maxRequests;
    	
    	keepAlive = true;

    	headers = new MimeHeaders();
    	
    	responseHeaders = new MimeHeaders();
    
   	}

    /**
     * Needed by VelocityFilter.Vrequest.  Should not be used to create
     * a <code>Request</code> object.
     */
    protected Request() {}

    /**
     * Returns a string representation of this <code>Request</code>.
     * The string representation is the first line (the method line) of the
     * HTTP request that this <code>Request</code> is handling.  Useful for
     * debugging.
     *
     * @return	The string representation of this <code>Request</code>.
     */
    public String toString()
    {
    	StringBuffer sb = new StringBuffer();

    	sb.append(method).append(' ').append(url);
    	
    	if ((query != null) && (query.length() > 0)) 
    	{
    		sb.append('?').append(query);
    	}
    	
    	sb.append(' ').append(protocol);
    	
    	return sb.toString();
    }
	
    /**
     * Reads an HTTP request from the socket.
     *
     * @return	<code>true</code> if the request was successfully read and
     *		parsed, <code>false</code> if the request was malformed.
     *
     * @throws	IOException
     *		if there was an IOException reading from the socket.  See
     *		the socket documentation for a description of socket
     *		exceptions.
     */
    public boolean getRequest()	throws IOException
    {
    	if (server.props.get("debugProps") != null) 
    	{
    		if (props != null) {
    			props.dump(false, "at beginning of getRequest");
    		}
    	}

		/*
		 * Reset state.
		 */
		 
		requestsLeft--;
		connectionHeader = "Connection";
	
		/* I don't think we need to do this
		while ((props = serverProps.getPrior()) != null) {
		    props.remove();
		}
		*/

		method = null;
		url = null;
		query = null;
		protocol = "HTTP/1.1";
		headers.clear();
		postData = null;

		statusCode = 200;
		statusPhrase = "OK";
		responseHeaders.clear();
		startMillis = System.currentTimeMillis();
		out.bytesWritten=0;

		/*
		 * Get first line of HTTP request (the method line).
		 */
	 
		String line;
		
		while (true) 
		{
		    line = in.readLine();
		    
		    if (line == null) 
		    {
		    	return false;
		    } 
		    else if (line.length() > 0) 
		    {
		    	break;
		    }
		    log(Server.LOG_INFORMATIONAL, "Skipping blank line");
		}

		log(Server.LOG_LOG, "Request " + requestsLeft + " " + line);

		try 
		{
			StringTokenizer st = new StringTokenizer(line);
			method = st.nextToken();
			url = st.nextToken();
			protocol = st.nextToken();
		} 
		catch (NoSuchElementException e) 
		{
			sendError(400, line, null);
			return false;
		}

		/*
			if ((method.equals("GET") == false)
				&& (method.equals("POST") == false)
				&& (method.equals("PUT") == false)) {
			    sendError(501, method, null);
			    return false;
			}
		*/	

		if (protocol.equals("HTTP/1.0")) 
		{
		    version = 10;
		} 
		else if (protocol.equals("HTTP/1.1")) 
		{
		    version = 11;
		}
		else 
		{
		    sendError(505, line, null);
		    return false;
		}
	    
		/*
		 * Separate query string from URL.
		 */

		int index = url.indexOf('?');
	
		if (index >= 0) 
		{
			query = url.substring(index + 1);
			url = url.substring(0, index);
		} 
		else 
		{
			query = "";
		}

		headers.read(in);

		/*
		 * Remember POST data.  "Transfer-Encoding: chunked" is not handled
		 * yet.
		 */
	
		String str;
	
		str = getRequestHeader("Content-Length");
		
		if (str != null) 
		{
			int len;
			try 
			{
				len=Integer.parseInt(str);
				
				if (len > server.maxPost) 
				{
					sendError(413, len + " bytes is too much data to post", null);
					return false;
				}
				
				postData = new byte[len];
			} 
			catch (Exception e) 
			{
				sendError(411, str, null);
				return false;
			} 
			catch (OutOfMemoryError e) 
			{
				sendError(413, str, null);
				return false;
			}
	    
			log(Server.LOG_DIAGNOSTIC, "Request", "Reading content: " + str);
	    
			in.readFully(postData);

		}

		str = getRequestHeader(connectionHeader);
	
		if ("Keep-Alive".equalsIgnoreCase(str)) 
		{
			keepAlive = true;
		} 
		else if ("close".equalsIgnoreCase(str)) 
		{
			keepAlive = false;
		} 
		else if (version > 10) 
		{
			keepAlive = true;
		} else 
		{
			keepAlive = false;
		}

		/*
	   * Delay initialization until we know we need these things
	   */
		serverProps = new PropertiesList(server.props);
		props = new PropertiesList();
		props.addBefore(serverProps);

		return true;
    }

    
    boolean
    shouldKeepAlive()
    {
    	return (requestsLeft > 0) && keepAlive;
    }

    /**
     * The socket from which the HTTP request was received, and to where the
     * HTTP response will be written.  The user should not directly read from
     * or write to this socket.  The socket is provided other purposes, for
     * example, imagine a handler that provided different content depending
     * upon the IP address of the client.
     *
     * @return	The client socket that issued this HTTP request.
     */
    public Socket
    getSocket()
    {
	return sock;
    }

    /**
     * Logs a message by calling <code>Server.log</code>.  Typically a
     * message is generated on the console or in a log file, if the
     * <code>level</code> is less than the current server log setting. 
     *
     * @param	level
     *		The severity of the message.  
     *
     * @param	message
     *		The message that will be logged.
     *
     * @see	Server#log(int, Object, String)
     */
    public void
    log(int level, String message)
    {
	log(level, null, message);
    }

    /**
     * Logs a message by calling <code>Server.log</code>.  Typically a
     * message is generated on the console or in a log file, if the
     * <code>level</code> is less than the current server log setting. 
     *
     * @param	level
     *		The severity of the message.  
     *
     * @param	obj
     *		The object that the message relates to.
     *
     * @param	message
     *		The message that will be logged.
     *
     * @see	Server#log(int, Object, String)
     */
    public void
    log(int level, Object obj, String message)
    {
	server.log(level, obj, message);
    }

    /*
     *-----------------------------------------------------------------------
     * Request methods.
     *-----------------------------------------------------------------------
     */

    /**
     * Returns the value that the given case-insensitive key maps to
     * in the HTTP request headers.  In order to do fancier things like
     * changing or deleting an existing request header, the user may directly
     * access the <code>headers</code> field.
     *
     * @param	key
     *		The key to look for in the HTTP request headers.  May not
     *		be <code>null</code>.
     *
     * @return	The value to which the given key is mapped, or
     *		<code>null</code> if the key is not in the headers.
     *
     * @see	#headers
     */
    public String
    getRequestHeader(String key)
    {
	return headers.get(key);
    }

   /**
     * Retrieves the query data as a hashtable.
     * This includes both the query information included as part of the url 
     * and any posted "application/x-www-form-urlencoded" data.
     *
     * @param   table
     *		An existing hashtable in which to put the query data as
     *		name/value pairs.  May be <code>null</code>, in which case
     *		a new hashtable is allocated.
     *
     * @returns	The hashtable in which the query data was stored.
     */
    public Hashtable  getQueryData(Hashtable table)
    {	
    	if (table == null) 
    	{
    		table = new Hashtable();
    	}
    	
    	HttpUtil.extractQuery(query, table);
    	
    	if (postData != null) 
    	{	
    		
    		String contentType = headers.get("Content-Type");
    		if ("application/x-www-form-urlencoded".equals(contentType)) 
    		{
    			HttpUtil.extractQuery(new String(postData), table);
    		}
    	}
    	
    	return table;
    	
    	
    	
    }

   /**
     * Retrieves the query data as a hashtable.
     * This includes both the query information included as part of the url 
     * and any posted "application/x-www-form-urlencoded" data.
     *
     * @returns	The hashtable in which the query data was stored.
     */
    public Hashtable
    getQueryData()
    {
    	return getQueryData(null);
    }

    /*
     *-----------------------------------------------------------------------
     * Response methods.
     *-----------------------------------------------------------------------
     */

    /**
     * Sets the status code of the HTTP response.  The default status
     * code for a response is <code>200</code> if this method is not
     * called.
     * <p>
     * An HTTP status phrase will be chosen based on the given
     * status code.  For example, the status code <code>404</code> will get
     * the status phrase "Not Found".
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	code
     *		The HTTP status code, such as <code>200</code> or
     *		<code>404</code>.  If &lt; 0, the HTTP status code will
     *		not be changed.
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    setStatus(int code)
    {
	if (code >= 0) {
	    setStatus(code, HttpUtil.getStatusPhrase(code));
	} 
    }

    /**
     * Set the HTTP status code and status phrase of this request.  The given
     * status will be sent to the client when the user directly or indirectly
     * calls the method <code>sendHeaders</code>.  The given status phrase
     * replaces the default HTTP status phrase normally associated with the
     * given status code.
     *
     * @param	code
     *		The HTTP status code, such as <code>200</code> or
     *		<code>404</code>.
     *
     * @param	message
     *		The HTTP status phrase, such as <code>"Okey dokey"</code> or
     *		<code>"I don't see it"</code>.
     *
     * @see	#sendHeaders(int, String, int)
     */
    protected void
    setStatus(int code, String message)
    {
	this.statusCode = code;
	this.statusPhrase = message;
    }

    /**
     * Return the status code.
     */

    public int getStatus() {
        return statusCode;
    }

    /**
     * Return uses of this socket
     */

    public int getReuseCount() {
	return server.maxRequests - requestsLeft;
    }

    /**
     * Adds a response header to the HTTP response.  In order to do fancier
     * things like appending a value to an existing response header, the
     * user may directly access the <code>responseHeaders</code> field.
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	key
     *		The header name.  
     *
     * @param	value
     *		The value for the request header.
     *
     * @see	#sendHeaders(int, String, int)
     * @see	#responseHeaders
     */
    public void
    addHeader(String key, String value)
    {
	responseHeaders.add(key, value);
    }

    /**
     * Adds a response header to the HTTP response.  In order to do fancier
     * things like appending a value to an existing response header, the
     * user may directly access the <code>responseHeaders</code> field.
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	line
     *		The HTTP response header, of the form
     *		"<code>key</code>: <code>value</code>".
     *
     * @see	#sendHeaders(int, String, int)
     * @see	#responseHeaders
     */
    public void
    addHeader(String line)
    {
	int dots = line.indexOf(':');
	String key = line.substring(0, dots);
	String value = line.substring(dots + 1).trim();
	addHeader(key, value);
    }

    /**
     * Sends an HTTP response to the client.  
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers, then sends the given byte array as the HTTP
     * response body. If the request method is HEAD, the body is not sent.
     * <p>
     * The "Content-Length" will be set to the length of the given byte array.
     * The "Content-Type" will be set to the given MIME type.
     *
     * @param	body
     *		The array of bytes to send as the HTTP response body.  May
     *		not be <code>null</code>.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to use the existing "Content-Type"
     *		response header (if any).
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client.
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    sendResponse(byte[] body, String type)
	throws IOException
    {
	sendHeaders(-1, type, body.length);
	if (!method.equals("HEAD")) {
	    out.write(body);
	}
    }

    /**
     * Sends an HTTP response to the client.  
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers.  It then writes out the given string to the client
     * as a sequence of bytes.  Each character in the string is written out
     * by discarding its high eight bits.
     * <p>
     * The "Content-Length" will be set to the length of the string.
     * The "Content-Type" will be set to the given MIME type.
     * <p>
     * Note: to use a different character encoding, use
     * <code>sendResponse(body.getBytes(encoding)...)</code> instead.
     *
     * @param	body
     *		The string to send as the HTTP response body.  May
     *		not be <code>null</code>. If the request method is HEAD,
     *          the body is not sent.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client. 
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    sendResponse(String body, String type, int code)
	throws IOException
    {
	sendHeaders(code, type, body.length());
	if (!"HEAD".equals(method)) {
	    out.writeBytes(body);
	}
    }

    /**
     * Convenience method that sends an HTTP response to the client
     * with a "Content-Type" of "text/html" and the default HTTP status
     * code.
     *
     * @param	body
     *		The string to send as the HTTP response body.
     *
     * @see	#sendResponse(String, String, int)
     */
    public void
    sendResponse(String body)
	throws IOException
    {
	sendResponse(body, "text/html", -1);
    }

    /**
     * Convenience method that sends an HTTP response to the client
     * with the default HTTP status code.
     * 
     * @param	body
     *		The string to send as the HTTP response body.  
     *		If the request method is HEAD,
     *          only the headers are sent to the client.
     *
     * @param	type
     *		The MIME type of the response.
     *
     * @see	#sendResponse(String, String, int)
     */
    public void
    sendResponse(String body, String type)
	throws IOException
    {
	sendResponse(body, type, -1);
    }

    /**
     * Sends the contents of the given input stream as the HTTP response.
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers.  It then transfers a total of <code>length</code>
     * bytes of data from the given input stream to the client as the
     * HTTP response body.
     * <p>
     * This method takes care of setting the "Content-Length" header
     * if the actual content length is known, or the "Transfer-Encoding"
     * header if the content length is not known (for HTTP/1.1 clients only).
     * <p>
     * This method may set the <code>keepAlive</code> to <code>false</code>
     * before returning, if fewer than <code>length</code> bytes could be
     * read. If the request method is HEAD, only the headers are sent.
     *
     * @param	in
     *		The input stream to read from.  
     *
     * @param	length
     *		The content length.  The number of bytes to send to the
     *		client.  May be &lt; 0, in which case this method will read
     *		until reaching the end of the input stream.
     * 
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client. 
     */
    public void
    sendResponse(InputStream in, int length, String type, int code)
	throws IOException
    {
	HttpInputStream hin = new HttpInputStream(in);

	byte[] buf = new byte[server.bufsize];
	    
	if (length >= 0) {
	    sendHeaders(code, type, length);
	    if (!method.equals("HEAD")) {
	       if (hin.copyTo(out, length, buf) != length) {
		   keepAlive = false;
	       }
	    }
	} else if (version <= 10) {
	    keepAlive = false;
	    sendHeaders(code, type, -1);
	    if (!method.equals("HEAD")) {
	       hin.copyTo(out, -1, buf);
	    }
	} else {
	    if (method.equals("HEAD")) {
	        sendHeaders(code, type, -1);
		return;
	    }

	    addHeader("Transfer-Encoding", "chunked");
	    sendHeaders(code, type, -1);

	    while (true) {
		int count = hin.read(buf);
		if (count < 0) {
		    out.writeBytes("0\r\n\r\n");
		    break;
		}
		out.writeBytes(Integer.toHexString(count) + "\r\n");
		out.write(buf, 0, count);
		out.writeBytes("\r\n");
	    }
	}
    }

    /**
     * Sends a HTTP error response to the client.  
     *
     * @param	code
     *		The HTTP status code.
     *
     * @param	clientMessage
     *		A short message to be included in the error response
     *		and logged to the server.
     */
    public void
    sendError(int code, String clientMessage)
    {
	sendError(code, clientMessage, null);
    }

    /**
     * Sends a HTTP error response to the client.  
     *
     * @param	code
     *		The HTTP status code.
     *
     * @param	clientMessage
     *		A short message to be included in the error response.
     *
     * @param	logMessage
     *		A short message to be logged to the server.  This message is
     *		<b>not</b> sent to the client.
     */
    public void
    sendError(int code, String clientMessage, String logMessage)
    {
	setStatus(code);
	server.errorCount++;

	String message = clientMessage;
	if (message == null) {
	    message = logMessage;
	    logMessage = null;
	}
	log(Server.LOG_LOG, "Error",
		statusCode + " " + statusPhrase + ": " + message);
	if (logMessage != null) {
	    log(Server.LOG_LOG, logMessage);
	}

	keepAlive = false;
	if (headersSent) {
	    /*
	     * The headers have already been sent.  We can't send an error
	     * message in the middle of an existing response, so just close
	     * this request.
	     */

	    return;
	}

        String body = "<html>\n<head>\n"
		+ "<title>Error: " + statusCode + "</title>\n"
		+ "<body>\nGot the error: <b>"
		+ statusPhrase
                + "</b><br>\nwhile trying to obtain <b>"
                + ((url == null) ? "unknown URL" : HttpUtil.htmlEncode(url))
                + "</b><br>\n"
                + HttpUtil.htmlEncode(clientMessage)
                + "\n</body>\n</html>";
 
        try {
	    sendResponse(body, "text/html", statusCode);
        } catch (IOException e) {
            /*
             * Don't throw an error in the process of sending an error
             * message!
             */
        }
    }

    /**
     * Sends the HTTP status line and response headers to the client.  This
     * method is automatically invoked by <code>sendResponse</code>, but
     * can be manually invoked if the user needs direct access to the
     * client's output stream.  If this method is not called, then the
     * HTTP status and response headers will not automatically be sent to
     * the client; the user would be responsible for forming the entire
     * HTTP response.
     * <p>
     * The user may call the <code>addHeader</code> method or modify the
     * <code>responseHeaders</code> field before calling this method.
     * This method then adds a number of HTTP headers, as follows: <ul>
     * <li> "Date" - the current time, if this header is not already present.
     * <li> "Server" - the server's name (from <code>server.name</code>), if
     *	    this header is not already present.
     * <li> "Connection" - "Keep-Alive" or "close", depending upon the
     *	    <code>keepAlive</code> field.
     * <li> "Content-Length" - set to the given <code>length</code>.
     * <li> "Content-Type" - set to the given <code>type</code>.
     * </ul>
     * <p>
     * The string used for "Connection" header actually comes from the
     * <code>connectionHeader</code> field.
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	length
     *		The length of the response body.  May be &lt; 0 if the length
     *		is unknown and/or to preserve the existing "Content-Length"
     *		response header (if any).
     *
     * @throws	IOException
     *		if there was an I/O error while sending the headers to
     *		the client. 
     *
     * @see	#setStatus(int)
     * @see	#addHeader(String, String)
     * @see	#sendResponse(String, String, int)
     * @see	#connectionHeader
     */
    public void
    sendHeaders(int code, String type, int length)
	throws IOException
    {
	setStatus(code);
        if ((length == 0) && (statusCode == 200)) {
	    /*
	     * No Content.
	     */
            setStatus(204);
        }

	responseHeaders.putIfNotPresent("Date", HttpUtil.formatTime());
	if (server.name != null) {
	    responseHeaders.putIfNotPresent("Server", server.name);
	}
	String str = shouldKeepAlive() ? "Keep-Alive" : "close";
	responseHeaders.put(connectionHeader, str);
        if (length >= 0) {
	    responseHeaders.put("Content-Length", Integer.toString(length));
	}
	if (type != null) {
	    responseHeaders.putIfNotPresent("Content-Type", type);
	}

	out.sendHeaders(this);
	// Logger.out.println("*** Sending headers: " +url+ " " + responseHeaders);
	headersSent = true;
    }

    /**
     * Send the response headers to the client.
     *  This consists of standard plus added headers.  The handler is reponsible
     *  for sending the reponse body.
     *
     * @param type	The document mime type
     * @param length	the document length
     * 
     * @see Request#addHeader(String)
     * @see Request#sendResponse(String)
     * @see Request#setStatus(int)
     */


    /**
     * Responds to an HTTP request with a redirection reply, telling the
     * client that the requested url has moved.  Generally, this is used if
     * the client did not put a '/' on the end of a directory.
     *
     * @param	url
     *		The URL the client should have requested.  This URL may be
     *		fully-qualified (in the form "http://....") or host-relative
     *		(in the form "/...").
     *
     * @param	body
     *		The body of the redirect response, or <code>null</code> to
     *		send a hardcoded message.
     */
    public void
    redirect(String url, String body)
	throws IOException
    {
	if (url.startsWith("/")) {
	    url = serverUrl() + url;
	}
	addHeader("Location", url);
	if (body == null) {
	    body = "<title>Moved</title><h1>look for <a href=" +
		url + ">" + url + "</h1>";
	}
	sendResponse(body, "text/html", 302);
    }

    /**
     * Returns the server's fully-qualified base URL.  This is "http://"
     * followed by the server's hostname and port.
     * <p>
     * If the HTTP request header "Host" is present, it specifies the
     * hostname and port that will be used instead of the server's internal
     * name for itself.  Due bugs in certain browsers, when using the server's
     * internal name, the port number will be elided if it is <code>80</code>.
     *
     * @return	The string representation of the server's URL.
     */
    public String
    serverUrl()
    {
	String host = headers.get("Host");
	if (host == null) {
	    host = server.hostName;
	}
	int index = host.lastIndexOf(":");
	if ((index < 0) && (server.listen.getLocalPort() != 80)) {
	    host += ":" + server.listen.getLocalPort();
	}
	return server.protocol + "://" + host;
    }

    /**
     * The <code>HttpOutputStream</code> provides the convenience method
     * <code>writeBytes</code> for writing the byte representation of a
     * string, without bringing in the overhead and the deprecated warnings
     * associated with a <code>java.io.DataOutputStream</code>.
     * <p>
     * The other methods in this class are here to allow the 
     * <code>FilterHandler</code> and <code>ChainSawHandler</code> to
     * alter the behavior in an implememtation specific way.  This behavior
     * is unfortunate, and might go away when a better strategy comes along.
     */
    public static class HttpOutputStream
	extends FilterOutputStream
    {
        /**
         * Count the number of bytes that are written to this stream
         */

        public int bytesWritten = 0;

	public
	HttpOutputStream(OutputStream out)
	{
	    super(out);
	}

	public void
	writeBytes(String s)
	    throws IOException
	{
	    int len = s.length();
	    for (int i = 0; i < len; i++) {
		this.out.write((byte) s.charAt(i));
	    }
	    bytesWritten += len;
	}

        public void write(byte b) throws IOException {
            this.out.write(b);
	    bytesWritten++;
        }

        public void
        write(byte[] buf, int off, int len) throws IOException {
            this.out.write(buf, off, len);
	    bytesWritten += len;
        }

	public void
	sendHeaders(Request request)
	    throws IOException
	{
	    writeBytes(request.protocol + " " + request.statusCode + " " +
		    request.statusPhrase + "\r\n");
	    request.responseHeaders.print(this.out);
	    writeBytes("\r\n");
	}
    }

    /**
     * Adds the given <code>Dictionary</code> to the set of properties that
     * are searched by <code>request.props.getProperty()</code>.  This method
     * is used to optimize the case when the caller has an existing
     * <code>Dictionary</code> object that should be added to the search
     * chain.
     * <p>
     * Assume the caller is constructing a new <code>Properties</code>
     * object and wants to chain it onto the front of
     * <code>request.props</code>.  The following code is appropriate:
     * <code><pre>
     * /&#42; Push a temporary Dictionary onto request.props. &#42;/
     * PropertiesList old = request.props;
     * (new PropertiesList()).addBefore(request.props);
     * request.props = request.props.getPrior();
     * request.props.put("foo", "bar");
     * request.props.put("baz", "garply");
     *
     * /&#42; Do something that accesses new properties. &#42;/
     *     .
     *     .
     *     .
     *
     * /&#42; Restore old Dictionary when done. &#42;/
     * request.props.remove();
     * request.props = old;
     * </pre></code>
     * However, <code>addSharedProps</code> may be called when the caller
     * has an existing set of <code>Properties</code> and is faced with
     * copying its contents into <code>request.props</code> and/or trying
     * to share the existing <code>Properties</code> object among multiple
     * threads concurrently.
     * <code><pre>
     * /&#42; Some properties created at startup. &#42;/
     * static Properties P = new Properties();
     *     .
     *     .
     *     .
     * /&#42; Share properties at runtime. &#42;/
     * request.addSharedProps(P);
     * </pre></code>is more efficient and esthetically pleasing than:
     * <code><pre>
     * foreach key in P.keys() {
     *     request.props.put(key, P.get(key));
     * }
     * </pre></code>
     * The given <code>Dictionary</code> object is added to the
     * <code>Properties.getProperty()</code> search chain before serverProps;
     * it will be searched after the 
     * <code>request.props</code> and before <code>serverProps</code>.
     * Multiple <code>Dictionary</code> objects can be added and they will
     * be searched in the order given.  The same <code>Dictionary</code>
     * object can be added multiple times safely.  However, the search
     * chain for the given <code>Dictionary</code> must not refer back to
     * <code>request.props</code> itself or a circular chain will be
     * created causing an infinite loop:
     * <code><pre>
     * request.addSharedProps(request.props);	            // Bad
     * request.addSharedProps(request.props.getWrapped());  // Good
     * Properties d1 = new Properties(request.props);
     * request.addSharedProps(d1);                          // Bad
     * Hashtable d2 = new Hashtable();
     * Properties d3 = new Properties();
     * request.addSharedProps(d2);		            // Good
     * request.addSharedProps(d3);		            // Good
     * </pre></code>
     * Subsequent calls to <code>request.props.getProperty()</code> may
     * fetch properties from an added <code>Dictionary</code>, but
     * <code>request.put()</code> will <b>not</b> modify those dictionaries.
     *
     * @param	d
     *		A <code>Dictionary</code> of <code>String</code> key/value
     *		pairs that will be added to the chain searched
     *		when <code>request.props.getProperty()</code> is called.  The
     *		dictionary <code>d</code> is "live", meaning that external
     *		changes to the contents of <code>d</code> will be seen on
     *		subsequent calls to <code>request.props.getProperty()</code>.
     *
     * @return	<code>false</code> if the dictionary had already been added
     *		by a previous call to this method, <code>true</code>
     *		otherwise.
     */
    public boolean
    addSharedProps(Dictionary d)
    {
	boolean debug = server.props.get("debugProps") != null;
	if (props.wraps(d) != null) {
	    if (debug) {
		Logger.out.println("addSharedProps didn't add dict"
			   + Integer.toHexString(System.identityHashCode(d)));
	    }
	    return false;
	}
	PropertiesList pl = new PropertiesList(d);
	pl.addBefore(serverProps);
	if (debug) {
	    props.dump(true, "at addSharedProps");
	}
	return true;
    }

    /**
     * Removes a <code>Dictionary</code> added by
     * <code>addSharedProps</code>.  <code>Dictionary</code> objects may
     * be removed in any order.  <code>Dictionary</code> objects do not need
     * to be removed; they will automatically get cleaned up at the end of
     * the request.
     *
     * @param	d
     *		The <code>Dictionary</code> object to remove from the
     *		<code>request.props.getProperty()</code> search chain.
     *
     * @return	<code>true</code> if the <code>Dictionary</code> was found
     *		and removed, <code>false</code> if the <code>Dictionary</code>
     *		was not found (it had already been removed or had never been
     *		added).
     */
    public boolean
    removeSharedProps(Dictionary d)
    {
	PropertiesList pl = props.wraps(d);
	if (pl != null && pl != props && pl != serverProps) {
	    pl.remove();
	    if (server.props.get("debugProps") != null) {
		pl.dump(true, "at removeSharedProps");
	    }
	    return true;
	}
	return false;
    }
}
