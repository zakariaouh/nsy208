/*
 * MimeHeaders.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 1999 Sun Microsystems, Inc.
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
 * The Initial Developer of the Original Code is: cstevens.
 * Portions created by cstevens are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, suhler.
 *
 * Version:  1.12
 * Created by cstevens on 99/09/15
 * Last modified by cstevens on 99/11/16 14:48:57
 */

package sunlabs.brazil.util.http;

import sunlabs.brazil.util.StringMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class is build on top of the <code>StringMap</code> class and
 * provides added methods that are of help when manipulating MIME headers.
 * By creating an instance of this class, the user can conveniently read,
 * write, and modify MIME headers.
 *
 * @author      Colin Stevens (colin.stevens@sun.com)
 * @version	1.12 99/11/16
 */

public class MimeHeaders
    extends StringMap
{
    /**
     * Creates a new, empty <code>MimeHeaders</code> object.
     */
    public
    MimeHeaders()
    {
    }

    /**
     * Creates a new <code>MimeHeaders</code> object and then initializes
     * it by reading MIME headers from the specified input stream.
     *
     * @param	in
     *		The input stream to read.
     */
    public
    MimeHeaders(HttpInputStream in)
	throws IOException
    {
	read(in);
    }

    /**
     * Reads MIME headers from the specified input stream.  This method
     * reads up to and consumes the blank line that marks the end of the
     * MIME headers.  It also stops reading if it reaches the end of
     * the input stream.
     * <p>
     * The MIME headers read from the input stream are stored in this
     * <code>MimeHeaders</code> object.  All headers read are <b>added</b>
     * to the existing headers; the new headers do not replace the
     * existing ones.  The order of the headers in this object will
     * reflect the order of the headers from the input stream, but 
     * space characters surrounding the keys and values are not preserved.
     * <p>
     * In a set of MIME headers, the given key may appear multiple times
     * (that is, on multiple lines, not necessarily consecutively).
     * In that case, that key will appear multiple times in this
     * <code>MimeHeaders</code> object also.  The HTTP spec says that
     * if a given key appears multiple times in a set of MIME headers, the
     * values can be concatenated together with commas
     * between them.  However, in practice, it appears that some browsers
     * and HTTP servers get confused when encountering such collapsed
     * MIME headers, for instance, the Yahoo mail reader program.  
     * <p>
     * MIME headers also support the idea of continuation lines, where
     * a key (and optionally its value) is followed on subsequent line(s) by
     * another value without a key.  The HTTP spec says that in this case
     * the values can be concatenated together with space characters
     * between them.  In practice, joining continuation lines together
     * does not seem to confuse any browsers or HTTP servers.  This method
     * joins continuation lines together by putting the space-equivalent
     * characters "\r\n\t" between the values so it can be easily parsed
     * with <code>StringTokenizer</code> and also easily written to
     * an output stream in a format that actually preserves its formatting
     * as a continuation line.
     *
     * @param	in
     *		The input stream to read from.
     *
     * @throws	IOException
     *		if the input stream throws an IOException while being
     *		read.
     */
    public void
    read(HttpInputStream in)
	throws IOException
    {
	while (true) {
	    String line = in.readLine();
	    if ((line == null) || (line.length() == 0)) {
		break;
	    }

	    if (Character.isSpaceChar(line.charAt(0)) == false) {
		int index = line.indexOf(':');
		if (index >= 0) {
		    String key = line.substring(0, index).trim();
		    String value = line.substring(index + 1).trim();
		    add(key, value);
		}
	    } else if (size() > 0) {
		String value = get(size() - 1);
		put(size() - 1, value + "\r\n\t" + line.trim());
	    }
	}
    }

    /**
     * Writes this <code>MimeHeaders</code> object to the given output
     * stream.  This method does <code>not</code> write a blank line after
     * the headers are written.
     *
     * @param	out
     *		The output stream.
     */
    public void
    print(OutputStream out)
    {
	print(new PrintStream(out));
    }

    /**
     * Writes this <code>MimeHeaders</code> object to the given output
     * stream.  This method does <code>not</code> write a blank line after
     * the headers are written.
     *
     * @param	out
     *		The output stream.
     */
    public void
    print(PrintStream out)
    {
	int length = size();
	for (int i = 0; i < length; i++) {
	    String key = getKey(i);
	    String value = get(i);
	    out.print(key + ": " + value + "\r\n");
	}
    }

    /**
     * Maps the given case-insensitive key to the specified value if the
     * key does not already exist in this <code>MimeHeaders</code> object.
     * <p>
     * Often, when dealing with MIME headers, the user will want to set
     * a header only if that header is not already set.  
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.  May be <code>null</code>.
     */
    public void
    putIfNotPresent(String key, String value)
    {
	if (get(key) == null) {
	    put(key, value);
	}
    }

    /**
     * Maps the given case-insensitive key to the specified value in this
     * <code>MimeHeaders</code> object, replacing the old value.
     * <p>
     * This is convenience method that automatically converts the
     * integer value to a string before calling the underlying
     * <code>put</code> method.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.
     */
    public void
    put(String key, int value)
    {
	put(key, Integer.toString(value));
    }

    /**
     * Adds a mapping for the given case-insensitive key to the specified
     * value in this <code>MimeHeaders</code> object.  It leaves any existing
     * key-value mapping alone.
     * <p>
     * This is convenience method that automatically converts the
     * integer value to a string before calling the underlying
     * <code>add</code> method.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.
     */
    public void
    add(String key, int value)
    {
	add(key, Integer.toString(value));
    }

    /**
     * Copies the contents of this <code>MimeHeaders</code> object,
     * {@link #add adding} all the other's keys and values to the other.
     *
     * @param	other
     *		The <code>MimeHeaders</code> object to copy to.
     */
    public void
    copyTo(MimeHeaders other)
    {
	int length = size();
	for (int i = 0; i < length; i++) {
	    other.add(getKey(i), get(i));
	}
    }
}
