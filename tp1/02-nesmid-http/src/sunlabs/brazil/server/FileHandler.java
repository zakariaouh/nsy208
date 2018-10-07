/*
 * FileHandler.java
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
 * Version:  1.34
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/07/24 10:47:55
 */

package sunlabs.brazil.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

import nesmid.util.HttpUtil;

/**
 * Standard handler for fetching static files.
 * This handler does URL to file conversion, file suffix to mime type
 * lookup, delivery of <i>index</i> files where providing directory
 * references, and redirection for missing slashes (/) at the end of
 * directory requests.
 * <p>
 * The following coniguration parameters are used:
 * <dl class=props>
 * <dt>root	<dd> property for document root (.)
 *		Since the document root is common to many handlers, if no
 *		root property is found with the supplied prefix, then the
 *		root property with the empty prefix ("") is used instead.
 *		This allows many handlers to share the common property.
 * <dt>default	<dd> The document to deliver 
 *		if the URL ends in "/". (defaults to index.html.)
 * <dt>prefix	<dd> Only url's that start with this are allowed.
 *		defaults to "".  The prefix is removed from the
 *		url before looking it up in the file system.
 *		So, if prefix is <code>/foo</code> then the the file
 *		<code>[root]/foo/bar.html</code> will be delivered
 *		in response to the url <code>/bar.html</code>.
 * <dt>mime	<dd> property for mime type
 *		For each file suffix .XX, the property mime.XX is used to 
 *		determine the mime type.  If no property exists, the document
 *		will not be delivered.
 * <dt>getOnly  <dd>If defined, only "GET" requests will be processed.  By
 *		default, all request types are handled. (Note: this is the
 *		inverse of the previous policy, defined by the undocumented
 *		"allow" parameter).
 * </dl>
 * <p>
 * The FileHandler sets the following entries in the request properties
 * as a side-effect:
 * <dl>
 * <dt>fileName		<dd>The absolute path of the file
 *			that couldn't be found.
 * <dt>DirectoryName	<dd>If the URL specified is a directory name, 
 *			its absolute path is placed here.
 * <dt>lastModified	<dd>The Time stamp of the last modified time
 * </dl>
 *
 * @author      Stephen Uhler
 * @version	1.34, 02/07/24
 */

public class FileHandler implements Handler 
{
    private static final String PREFIX = "prefix";  // our prefix
    private static final String DEFAULT = "default";    // property for default document, given directory
    private static final String GETONLY = "getOnly";  // allow only GETs

    public static final String MIME = "mime";	// property for mime type
    public static final String ROOT = "root";   // property for document root

    public String urlPrefix = "/";

    String prefix;


    /**
     * Initialize the file handler.
     *
     * @return	The file handler always returns true.
     */

    public boolean
    init(Server server, String prefix)
    {
	this.prefix = prefix;
	urlPrefix = server.props.getProperty(prefix + PREFIX, urlPrefix);
	return true;
    }

    /** 
     * Find, read, and deliver via http the requested file.
     * The server property <code>root</code> is used as the document root.
     * The document root is recalculated for each request, so an upstream
     * handler may change it for that request.
     * For URL's ending with "/", the server property <code>default</code>
     * (normally index.html) is automatically appended.
     *
     * If the file suffix is not found as a server property <code>
     * mime.<i>suffix</i></code>, the file is not delivered.
     */

    public boolean
    respond(Request request)
	throws IOException
    {
	if (!request.url.startsWith(urlPrefix)) {
	    return false;
	}
	if ((request.props.getProperty(prefix + GETONLY)!=null) &&
		(!request.method.equals("GET"))) {
	    request.log(Server.LOG_INFORMATIONAL, prefix, 
		"Skipping request, only GET's allowed");
	    return false;
	}	

	String url = request.url.substring(urlPrefix.length());
	Properties props = request.props;
	String root = props.getProperty(prefix + ROOT,
		props.getProperty(ROOT, "."));
	String name = urlToPath(url);
	request.log(Server.LOG_DIAGNOSTIC, prefix, "Looking for file: (" +
		root + ")(" + name + ")");
	File file = new File(root + name);
	String path = file.getPath();

	if (file.isDirectory()) {
	    /*
	     * Must check if the original <code>name</code> ends with "/",
	     * not <code>File.getPath</code> because in jdk-1.2,
	     * <code>File.getPath</code> truncates the terminating "/".
	     */

	    if (request.url.endsWith("/") == false) {
		request.redirect(request.url + "/", null);
		return true;
	    }
	    props.put("DirectoryName", path);

	    String index = props.getProperty(prefix + DEFAULT, "index.html");

	    file = new File(file, index);
	    path = file.getPath();
	}

	/*
	 * Put the name of the file in the request object.  This
	 * may be of some use for down stream handlers.  
	 */

	props.put("fileName", path);

	if (file.exists() == false) {
	    request.log(Server.LOG_INFORMATIONAL, prefix, 
		     "no such file: " + path);
	    return false;
	}

	String basename = file.getName();
	int index = basename.lastIndexOf('.');
	if (index < 0) {
	    request.log(Server.LOG_INFORMATIONAL, prefix,
		    "no file suffix for: " + path);
	    return false;
	}

	String suffix = basename.substring(index);
	String type = props.getProperty(prefix + MIME + suffix,
		props.getProperty(MIME + suffix));
	if (type == null) {
	    request.log(Server.LOG_INFORMATIONAL, prefix,
		    "unknown file suffix: " + suffix);
	    return false;
	}
	sendFile(request, file, 200, type);
	return true;
    }

    /**
     * Helper function to convert an url into a pathname. <ul>
     * <li> Collapse all %XX sequences.
     * <li> Ignore missing initial "/".
     * <li> Collapse all "/..", "/.", and "//" sequences.
     * </ul>
     * <code>URL(String)</code> collapses all "/.." (and "/.") sequences,
     * except for a trailing "/.." (or "/."), which would lead to the
     * possibility of escaping from the document root.
     * <p>
     * <code>File.getPath</code> in jdk-1.1 leaves all the "//" constructs
     * in, but it collapses them in jdk-1.2, so we have to always take it
     * out ourselves, just to be sure.
     *
     * @param	url
     *		The file path from the URL (that is, minus the "http://host"
     *		part).  May be <code>null</code>.
     *
     * @returns	The path that corresponds to the URL.  The returned value
     *		begins with "/".  The caller can concatenate this path
     *		onto the end of some document root.
     */
    public static String
    urlToPath(String url)
    {
	String name = HttpUtil.urlDecode(url);

	StringBuffer sb = new StringBuffer();
	StringTokenizer st = new StringTokenizer(name, "/");
	while (st.hasMoreTokens()) {
	    String part = st.nextToken();
	    if (part.equals(".")) {
		continue;
	    } else if (part.equals("..")) {
		for (int i = sb.length(); --i >= 0; ) {
		    if (sb.charAt(i) == '/') {
			sb.setLength(i);
			break;
		    }
		}
	    } else {
		sb.append(File.separatorChar).append(part);
	    }
	}
	if ((sb.length() == 0) || name.endsWith("/")) {
	    sb.append(File.separatorChar);
	}
	return sb.toString();
    }

    /**
     * Send a file as a response.
     * @param request       The request object
     * @param fileHandle    The file to output
     * @param type          The mime type of the file
     */

    static public void
    sendFile(Request request, File file, int code, String type)
	throws IOException
    {
	if (file.isFile() == false) {
	    request.sendError(404, null, "not a normal file");
	    return;
	}
	if (file.canRead() == false) {
	    request.sendError(403, null, "Permission Denied");
	    return;
	}

	FileInputStream in = null;
	try {
	    in = new FileInputStream(file);

	    request.addHeader("Last-Modified", 
		    HttpUtil.formatTime(file.lastModified()));
	    request.props.put("lastModified", "" + file.lastModified());

	    int size = (int) file.length();
	    request.setStatus(code);
	    size = range(request, in, size);
	    request.sendResponse(in, size, type, -1);
	} finally {
	    if (in != null) {
		in.close();
	    }
	}
    }

    /**
     * Compute simple byte ranges. (for gnutella support)
     * @returns		The (potential partial) size.
     *			The code may be modified as a side effect.
     */

    private static int
    range(Request request, FileInputStream in, int size) throws IOException {
	String range=request.getRequestHeader("range");
	int sep = 0;
	if (range != null && request.getStatus()==200 &&
		range.indexOf("bytes=") == 0 &&
		(sep = range.indexOf("-")) > 0 &&
		range.indexOf(",")<0) {
	    int start = -1;
	    int end = -1;
	    try {
		start = Integer.parseInt(range.substring(6,sep));
	    } catch (NumberFormatException e) {}
	    try {
		end = Integer.parseInt(range.substring(sep+1));
	    } catch (NumberFormatException e) {}
	    if (end == -1) {
		end = size;
	    } else if (end > size) {
		end = size;
	    }
	    if (start == -1) {
		start = size - end + 1;
		end = size;
	    }
	    if (end >= start) {
		in.skip(start);
		size = end - start + 1;
		request.setStatus(206);
	    }
	}
	return size;
    }
}
