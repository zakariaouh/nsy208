/*
 * Glob.java
 *
 * Brazil project web application Framework,  export version: 1.0
 * Copyright (c) 1999 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version 
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 * 
 * The Original Code is from: Brazil project web application Framework release 1.0.
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, suhler.
 *
 * Version:  1.6
 * Created by suhler on 99/08/05
 * Last modified by cstevens on 99/11/09 20:24:39
 */

package sunlabs.brazil.util;

/**
 * Glob-style string matching and substring extraction.  Glob was
 * implemented by translating the glob package for
 * <a href="http://www.scriptics.com">tcl8.0</a>.
 * <ul>
 * <li> "*" matches 0 or more characters
 * <li> "?"  matches a single character
 * <li> "[...]" matches a set and/or range of characters
 * <li> "\" following character is not special
 * </ul>
 *
 * Each of the substrings matching (?, *, or [..]) are returned.
 * 
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	1.6, 99/11/09
 */
public class Glob
{
    private Glob() {}

    /**
     * Match a string against a pattern.
     *
     * @param	pattern
     *		Glob pattern.
     *
     * @param	string
     *		String to match against pattern.
     *
     * @return	<code>true</code> if the string matched the pattern,
     *		<code>false</code> otherwise.
     */
    public static boolean
    match(String pattern, String string)
    {
	return match(pattern, string, null);
    }

    /**
     * Match a string against a pattern, and return sub-matches.
     * <p>
     * The caller can provide an array of strings that will be filled in with
     * the substrings of <code>string</code> that matched the glob
     * meta-characters in <code>pattern</code>.  The array of strings may be
     * partially modified even if the string did not match the glob pattern.
     * The array may contain more elements than glob meta-characters, in
     * which case those extra elements will not be modified; the array may
     * also contain fewer elements or even be <code>null</code>, to ignore
     * some or all of the glob meta-characters.  In other words, the user can
     * pass pretty much anything and this method defines errors out of
     * existence.
     *
     * @param	pattern
     *		Glob pattern.
     *
     * @param	string
     *		String to match against pattern.
     *
     * @param	substr
     *		Array of strings provided by the caller, to be filled in
     *		with the substrings that matched the glob meta-characters.
     *		May be <code>null</code>.
     *
     * @return	<code>true</code> if the string matched the pattern,
     *		<code>false</code> otherwise.
     */
    public static boolean
    match(String pattern, String string, String[] substr)
    {
	return match(pattern, 0, string, 0, substr, 0);
    }

    private static boolean
    match(String pat, int pIndex, String str, int sIndex, String[] substrs,
	    int subIndex)
    {
	int pLen = pat.length();
	int sLen = str.length();

	while (true) {
	    if (pIndex == pLen) {
		if (sIndex == sLen) {
		    return true;
		} else {
		    return false;
		}
	    } else if ((sIndex == sLen) && (pat.charAt(pIndex) != '*'))  {
		return false;
	    }

	    switch (pat.charAt(pIndex)) {
		case '*': {
		    int start = sIndex;
		    pIndex++;
		    if (pIndex >= pLen) {
			addMatch(str, start, sLen, substrs, subIndex);
			return true;
		    }
		    while (true) {
			if (match(pat, pIndex, str, sIndex, substrs,
				subIndex + 1)) {
			    addMatch(str, start, sIndex, substrs, subIndex);
			    return true;
			}
			if (sIndex == sLen) {
			    return false;
			}
			sIndex++;
		    }
		}
		case '?': {
		    pIndex++;
		    addMatch(str, sIndex, sIndex + 1, substrs, subIndex++);
		    sIndex++;
		    break;
		}
		case '[': {
		    try {
			pIndex++;
			char s = str.charAt(sIndex);
			char p = pat.charAt(pIndex);

			while (true) {
			    if (p == ']') {
				return false;
			    }
			    if (p == s) {
				break;
			    }
			    pIndex++;
			    char next = pat.charAt(pIndex);
			    if (next == '-') {
				pIndex++;
				char p2 = pat.charAt(pIndex);
				if ((p <= s) && (s <= p2)) {
				    break;
				}
				pIndex++;
				next = pat.charAt(pIndex);
			    }
			    p = next;
			}
			pIndex = pat.indexOf(']', pIndex) + 1;
			if (pIndex <= 0) {
			    return false;
			}
			addMatch(str, sIndex, sIndex + 1, substrs, subIndex++);
			sIndex++;
		    } catch (StringIndexOutOfBoundsException e) {
			/*
			 * Easier just to catch malformed [] sequences than
			 * to check bounds all the time.
			 */

			return false;
		    }
		    break;
		}
		case '\\': {
		    pIndex++;
		    if (pIndex >= pLen) {
			return false;
		    }
		    // fall through
		}
		default: {
		    if (pat.charAt(pIndex) != str.charAt(sIndex)) {
			return false;
		    }
		    pIndex++;
		    sIndex++;
		}
	    }
	}
    }

    private static void
    addMatch(String str, int start, int end, String[] substrs, int subIndex)
    {
	if ((substrs == null) || (subIndex >= substrs.length)) {
	    return;
	}

	substrs[subIndex] = str.substring(start, end);
    }
}
