/*
 * StringMap.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0 
 * Copyright (c) 1999-2000 Sun Microsystems, Inc.
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
 * Version:  1.11
 * Created by cstevens on 99/09/15
 * Last modified by cstevens on 00/03/29 16:46:29
 */

package sunlabs.brazil.util;

import java.util.Dictionary;    
import java.util.Enumeration;
import java.util.Vector;

/**
 * The <code>StringMap</code> class is a substitute for the Hashtable.
 * The StringMap has the following properties: <ul>
 * <li> Maps case-insensitive string keys to string values.
 * <li> The case of the keys is preserved.
 * <li> Values may be <code>null</code>.
 * <li> Preserves the relative order of the data.  
 * <li> The same key may appear multiple times in a single map.
 * <li> This map is implemented via a Vector, and as such, as the number of
 *      keys increases, the time required to search will go up.
 * </ul>
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	1.11, 00/03/29
 */
public class StringMap
    extends Dictionary
{
    Vector keys;
    Vector values;

    /**
     * Creates an empty StringMap.
     */
    public
    StringMap()
    {
	keys = new Vector();
	values = new Vector();
    }

    /**
     * Returns the number of elements in this StringMap.  Every occurrence of
     * keys that appear multiple times is counted.
     *
     * @return	The number of elements in this StringMap.
     *
     * @see	#keys
     *
     * @implements	Dictionary#size
     */
    public int
    size()
    {
	return keys.size();
    }

    /**
     * Tests if there are any elements in this StringMap.
     *
     * @return	Returns <code>true</code> if there are no elements,
     *		<code>false</code> otherwise.
     *
     * @implements	Dictionary#isEmpty
     */
    public boolean
    isEmpty()
    {
	return keys.isEmpty();
    }

    /**
     * Returns an enumeration of the keys in this StringMap.  The elements
     * of the enumeration are strings.
     * <p>
     * The same key may appear multiple times in the enumeration, not
     * necessarily consecutively.  Since <code>get</code> always returns
     * the value associated with the first occurrence of a given key, a
     * StringMap cannot be enumerated in the same fashion as a Hashtable.
     * Instead, the caller should use:
     * <pre>
     * Enumeration keys = map.keys();
     * Enumeration values = map.elements();
     * while (keys.hasMoreElements()) {
     *     String key = (String) keys.nextElement();
     *     String value = (String) values.nextElement();
     * }
     * </pre>
     * or:
     * <pre>
     * for (int i = 0; i < map.size(); i++) {
     *     String key = map.getKey(i);
     *     String value = map.get(i);
     * }
     * </pre>
     *
     * @return	An enumeration of the keys.
     * 
     * @see	#elements
     * @see	#size
     * @see	#getKey
     * @see	#get
     *
     * @implements	Dictionary#keys
     */
    public Enumeration
    keys()
    {
	return keys.elements();
    }

    /**
     * Returns an enumeration of the values in this StringMap.  The elements
     * of the enumeration are strings.
     * 
     * @return	An enumeration of the values.
     *
     * @see	#keys
     *
     * @implements	Dictionary#elements
     */
    public Enumeration
    elements()
    {
	return values.elements();
    }

    /**
     * Returns the key at the specified index.  The index ranges from
     * <code>0</code> to <code>size() - 1</code>.
     * <p>
     * This method can be used to iterate over all the keys in this
     * StringMap in the order in which they were inserted, subject to any
     * intervening deletions.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The key at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public String
    getKey(int index)
	throws IndexOutOfBoundsException
    {
	return (String) keys.elementAt(index);
    }

    /**
     * Returns the value at the specified index.  The index ranges from
     * <code>0</code> to <code>size() - 1</code>.
     * <p>
     * This method can be used to iterate over all the values in this
     * StringMap in the order in which they were inserted, subject to any
     * intervening deletions.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The value at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public String
    get(int index)
	throws IndexOutOfBoundsException
    {
	return (String) values.elementAt(index);
    }

    /**
     * Returns the value that the specified case-insensitive key maps to
     * in this StringMap.
     * <p>
     * The same key may appear multiple times in the enumeration; this
     * method always returns the value associated with the first
     * occurrence of the specified key.  In order to get all the values,
     * it is necessary to iterate over the entire StringMap to retrieve
     * all the values associated with a given key.
     *
     * @param	key
     *		A key in this StringMap.  May not be <code>null</code>.
     *
     * @return	The value to which the specified key is mapped, or
     *		<code>null</code> if the key is not in the StringMap.
     *
     * @see	#keys
     */
    public String
    get(String key)
    {
	int i = indexOf(key);
	if (i >= 0) {
	    return (String) values.elementAt(i);
	} else {
	    return null;
	}
    }

    /**
     * Performs the same job as <code>get(String)</code>.  It exists so
     * this class can extend the <code>Dictionary</code> class.
     * 
     * @param	key
     *		Must be a String.
     *
     * @return	A String value.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> is not a String.
     *
     * @see	#get(String)
     *
     * @implements	Dictionary#get
     */
    public Object
    get(Object key)
    {
	return get((String) key);
    }

    /**
     * Maps the key at the given index to the specified value in this
     * StringMap.  The index ranges from <code>0</code> to
     * <code>size() - 1</code>.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The value at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public void
    put(int index, String value)
    {
	values.setElementAt(value, index);
    }

    /**
     * Maps the given case-insensitive key to the specified value in this
     * StringMap.
     * <p>
     * The value can be retrieved by calling <code>get</code> with a
     * key that is case-insensitive equal to the given key.
     * <p>
     * If this StringMap already contained a mapping for the given key,
     * the old value is forgotten and the new specified value is used.
     * The case of the prior key is retained in that case.  Otherwise
     * the case of the new key is used.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.  May be <code>null</code>.
     *
     * @return	The previous value to which <code>key</code> was mapped,
     *		or <code>null</code> if the the key did not map to any
     *		value.
     */
    public void
    put(String key, String value)
    {
	int i = indexOf(key);
	if (i < 0) {
	    keys.addElement(key);
	    values.addElement(value);
	} else {
	    values.setElementAt(value, i);
	}
    }

    /**
     * Performs the same job as <code>put(String, String)</code>.  It exists
     * so this class can extend the <code>Dictionary</code> class.
     *
     * @param	key
     *		Must be a String.
     *
     * @param	value
     *		Must be a String.
     *
     * @return	The previous value to which <code>key</code> was mapped,
     *		or <code>null</code> if the the key did not map to any
     *		value.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> or <code>value</code> is not a
     *		String.
     *
     * @see	#put(String, String)
     *
     * @implements	Dictionary#put
     */
    public Object
    put(Object key, Object value)
    {
	String skey = (String) key;
	String svalue = (String) value;

	Object prior;

	int i = indexOf(skey);
	if (i < 0) {
	    prior = null;
	    keys.addElement(skey);
	    values.addElement(svalue);
	} else {
	    prior = values.elementAt(i);
	    values.setElementAt(svalue, i);
	}
	return prior;
    }

    /**
     * Maps the given case-insensitive key to the specified value in this
     * StringMap.
     * <p>
     * The new mapping is added to this StringMap even if the given key
     * already has a mapping.  In this way it is possible to create a key
     * that maps to two or more values.
     * <p>
     * Since the same key may appear multiple times in this StringMap, it
     * is necessary to iterate over the entire StringMap to retrieve all
     * values associated with a given key.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.  May be <code>null</code>.
     *
     * @see	#put(String, String)
     * @see	#keys
     */
    public void
    add(String key, String value)
    {
	keys.addElement(key);
	values.addElement(value);
    }

    /**
     * Removes the given case-insensitive key and its corresponding value
     * from this StringMap.  This method does nothing if the key is not in
     * this StringMap.
     * <p>
     * The same key may appear in multiple times in this StringMap; this
     * method only removes the first occurrence of the key.
     *
     * @param	key
     *		The key that needs to be removed.  Must not be
     *		<code>null</code>.
     */
    public void
    remove(String key)
    {
	int i = indexOf(key);
	if (i >= 0) {
	    remove(i);
	}
    }

    public void
    remove(int i)
    {
	keys.removeElementAt(i);
	values.removeElementAt(i);
    }

    /**
     * Performs the same job as <code>remove(String)</code>.  It exists so
     * this class can extend the <code>Dictionary</code> class.
     *
     * @param	key
     *		Must be a String.
     *
     * @return	The string value to which the key had been mapped, or
     *		<code>null</code> if the key did not have a mapping.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> is not a String.
     *
     * @implements	Dictionary#remove
     */
    public Object
    remove(Object key)
    {
	int i = indexOf((String) key);
	if (i >= 0) {
	    Object prior = values.elementAt(i);
	    remove(i);
	    return prior;
	}
	return null;
    }

    /**
     * Removes all the keys and values from this StringMap.
     */
    public void
    clear()
    {
	keys.setSize(0);
	values.setSize(0);
    }

    private int
    indexOf(String key)
    {
	int length = keys.size();
	for (int i = 0; i < length; i++) {
	    String got = (String) keys.elementAt(i);
	    if (key.equalsIgnoreCase(got)) {
		return i;
	    }
	}
	return -1;
    }

    /**
     * Returns a string representation of this <code>StringMap</code> in the
     * form of a set of entries, enclosed in braces and separated by the
     * characters ", ".  Each entry is rendered as the key, an equals sign
     * "=", and the associated value.
     *
     * @return	The string representation of this <code>StringMap</code>.
     */
    public String
    toString()
    {
	StringBuffer sb = new StringBuffer();

	sb.append('{');

	int length = keys.size();
	for (int i = 0; i < length; i++) {
	    sb.append(getKey(i));
	    sb.append('=');
	    sb.append(get(i));
	    sb.append(", ");
	}
	if (sb.length() > 1) {
	    sb.setLength(sb.length() - 2);
	}
	sb.append('}');

	return sb.toString();
    }
}
