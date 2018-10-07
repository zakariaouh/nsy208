package nesmid.util;


// import java.text.SimpleDateFormat; TINI
// import java.text.ParsePosition; TINI
import java.util.Hashtable;




/**
 * The <code>HttpUtil</code> class contains methods for performing simple
 * HTTP operations.
 *
 * @author      Colin Stevens (colin.stevens@sun.com)
 * @version	1.14 02/07/29
 */
public class HttpUtil
{
	
	
	
    private HttpUtil() {}

    /**
     * Which ascii characters may be sent in HTML without escaping
     */
    private static String[] htmlMap = new String[256];

    /**
     * Initialize <code>htmlMap</code> array
     * @see #htmlMap
     */
    static {
	for (int i = 0; i < 256; i++) {
	    if (i >= 32 && i < 126) {
		htmlMap[i]=null;
	    } else {
		htmlMap[i] = "#" + i;
	    }
	}
	htmlMap['"'] = "quot";
	htmlMap['&'] = "amp";
	htmlMap['<'] = "lt";
	htmlMap['>'] = "gt";
	htmlMap['\n'] = null;
	htmlMap['\r'] = null;
	htmlMap['\t'] = null;
    }

    /**
     * Converts a string into a valid HTML fragment.  Escapes the characters
     * <i>&quot;</i>, <i>&amp;</i>, <i>&lt;</i>, <i>&gt;</i>, and all
     * non-printables into the form <code>&amp;#xx;</code> (their "decimal
     * reference" form).
     *
     * @param	src
     *		The string to convert.
     *
     * @return	The string with all the special characters converted to
     *		decimal reference form.
     */
    public static String
    htmlEncode(String src)
    {
	StringBuffer result = new StringBuffer();

	int length = (src == null) ? 0 : src.length();
	for (int i = 0; i < length; i++) {
	    int ch = src.charAt(i) & 0xff;
	    if (htmlMap[ch]==null) {
		result.append((char) ch);
	    } else {
		result.append("&" + htmlMap[ch] + ";");
	    }
	}
	return result.toString();
    }

    private static final boolean[] safeUrl = new boolean[256];
    static {
	for (int i = 'a'; i <= 'z'; i++) {
	    safeUrl[i] = true;
	}
	for (int i = 'A'; i <= 'Z'; i++) {
	    safeUrl[i] = true;
	}
	for (int i = '0'; i <= '9'; i++) {
	    safeUrl[i] = true;
	}
	safeUrl['_'] = true;
	safeUrl[':'] = true;
	safeUrl['/'] = true;
	safeUrl['.'] = true;
	safeUrl['~'] = true;
    }

    /**
     * Maps a string to be used in a query or post into a form that is
     * acceptable in an URL.  Typically used when the caller wants to
     * safely generate an HREF containing an arbitrary string that may
     * have special characters.
     * <p>
     * URL strings may not contain non-alphanumeric characters.  All
     * non-alphanumeric characters are converted to the escape sequence
     * "%XX", where XX is the hexadecimal value of that character's code.
     * <p>
     * Note that the space character " " is NOT converted to "+".  That is
     * a common misconception.  "+" represents a space only in query strings,
     * not in the URL.  "%20" is how an actual space character must be
     * passed in an URL, and is also an acceptable way of passing a space in
     * a query string.
     *
     * @param	string
     *		The string to convert.
     *
     * @return	The URL-encoded version of the given string.
     */
    public static String
    urlEncode(String src)
    {
	StringBuffer result = new StringBuffer();
	int length = (src == null) ? 0 : src.length();
	for (int i = 0; i < length; i++) {
	    int ch = src.charAt(i) & 0xff;
	    if (safeUrl[ch]) {
		result.append((char) ch);
	    } else {
	    	result.append('%');
			result.append(Character.digit((char)((ch >> 4) & 0x0f), 16));
			result.append(Character.digit((char)(ch & 0x0f), 16));
	    }
	}

	return result.toString();
    }
	
    /**
     * Decodes a URL-encoded string by replacing all the "%XX" escape
     * sequences in the string with the corresponding character.
     * <p>
     * Malformed "%XX" sequences are silently ignored.
     *
     * @param	string
     *		The URL-encoded string.
     *
     * @return	The decoded version of the given string.
     */
    public static String
    urlDecode(String src)
    {
	if (src == null) {
	    return "";
	}
	int i = src.indexOf('%');
	if (i < 0) {
	    return src;
	}

	StringBuffer result = new StringBuffer(src.substring(0, i));
	int length = src.length();
	for ( ; i < length; i++) {
	    char ch = src.charAt(i);
	    if (ch == '%') {
		try {
		    ch = (char) Integer.parseInt(src.substring(i + 1, i + 3),
			    16);
		    i += 2;
		} catch (Exception e) {
		    // Ignore malformed % sequences, just insert the '%'.
		}
	    }
	    result.append(ch);
	}
	return result.toString();
    }
	
    /**  TINI
     * The format describing an http date.
     
    private static SimpleDateFormat dateFormat;
    static {
	dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
		Locale.US);
	dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
	dateFormat.setLenient(true);
    }
    */
    /**
     * Returns a string containing the current time as an HTTP-formatted
     * date.
     *
     * @return	HTTP date string representing the current time.
     */
    public static String
    formatTime()
    {
	return formatTime(System.currentTimeMillis());
    }

    /**
     * Returns a string containing an HTTP-formatted date.
     *
     * @param	time
     *		The date to format (current time in msec).
     *
     * @return	HTTP date string representing the given time.
     */
    public static String
    formatTime(long time)
    {
	return "TINI"; // TINI dateFormat.format(new Date(time)).substring(0, 29);
    }

    /**
     * Convert a last-modified date in "standard" format
     * into a time stamp.  This "inverses" formatTime.
     *
     * @param		time
     *			A correctly formatted HTTP date string.
     * @return	 	milliseconds since the epoch, or 0 if the conversion
     *			failed.
     */

    public static long
    parseTime(String time) {
	try {
	    return 0; // TINI dateFormat.parse(time.trim(),new ParsePosition(0)).getTime();
	} catch (Exception e) {
	    return 0;
	}
    }

    /**
     * Turns x-www-form-urlencoded form data into a dictionary.
     *
     * @param	query
     *		The x-www-form-urlencoded string.  May be <code>null</code>
     *
     * @param	table
     *		The dictionary to insert the form data into.
     */
    public static void
    extractQuery(String query, Hashtable table)
    {	
    	if(query.indexOf("<?xml version='1.0' encoding='UTF-8'?>") >=0)
    	{
    		table.put("xml", query);
    		return;
    	}
		
    	if (query == null) 
		{
		    return;
		}
		
		query = query.replace('+', ' ');
		
		StringTokenizer st = new StringTokenizer(query, "&");
		
		while (st.hasMoreTokens()) 
		{
		    String field = st.nextToken();
		    
		    int index = field.indexOf('=');
		    
		    if (index < 0) 
		    {
		    	table.put(urlDecode(field), "");
		    } 
		    else 
		    {
		    	table.put(urlDecode(field.substring(0, index)), urlDecode(field.substring(index + 1)));
		    }
		}
    }
    
    
    
    /*
     * Extract parts of a URL.
     * part: 1=protocol, 2=host, 3=port, 4=path
     */

    static SunlabsRegexp urlRe = new SunlabsRegexp("([^:]*)://([^:/]*)(:[0-9]+)?(.*)");
    static String parseUrl(String url, int part) {
	String matches[] = new String[5];
	return (urlRe.match(url, matches) ? matches[part] : null);
    }

    /**
     * Get the protocol portion of a Url String.
     * @return	null if the string is an invalid URL.
     */
    public static String extractUrlProtocol(String url) {
	return parseUrl(url, 1);
    }

    /**
     * Get the host portion of a Url String.
     * @return	null if the string is an invalid URL.
     */
    public static String extractUrlHost(String url) {
	return parseUrl(url, 2);
    }

    /**
     * Get the port portion of a Url String as a string.
     * @return	null if the string is an invalid URL, the empy string
     *		if no port was specified.
     */
    public static String extractUrlPort(String url) {
	String port = parseUrl(url, 3);
	return (port == null ? null : port.substring(1));
    }

    /**
     * Get the path portion of a Url String.
     * @return	null if the string is an invalid URL.
     */
    public static String extractUrlPath(String url) {
	String s = parseUrl(url, 4);
	if (s != null && s.equals("")) {
            return "/";
        } else {
            return s;
        }
    }

    /**
     * Returns the HTTP error string associated with the integer error code.
     * This error string can be used in HTTP responses.  Unknown codes
     * return the string "Error"
     *
     * @param	code
     *		The code to look up.
     *
     * @result	The associated error string.
     */
    public static String
    getStatusPhrase(int code)
    {
	switch (code) {
	    case 100:	return "Continue";
	    case 101:	return "Switching Protocols";
	    case 200:	return "OK";
	    case 201:	return "Created";
	    case 202:	return "Accepted";
	    case 203:	return "Non-Authoritative Information";
	    case 204:	return "No Content";
	    case 205:	return "Reset Content";
	    case 206:	return "Partial Content";
	    case 300:	return "Multiple Choices";
	    case 301:	return "Moved Permanently";
	    case 302:	return "Moved Temporarily";
	    case 303:	return "See Other";
	    case 304:	return "Not Modified";
	    case 305:	return "Use Proxy";
	    case 400:	return "Bad Request";
	    case 401:	return "Unauthorized";
	    case 402:	return "Payment Required";
	    case 403:	return "Forbidden";
	    case 404:	return "Not Found";
	    case 405:	return "Method Not Allowed";
	    case 406:	return "Not Acceptable";
	    case 407:	return "Proxy Authentication Required";
	    case 408:	return "Request Time-out";
	    case 409:	return "Conflict";
	    case 410:	return "Gone";
	    case 411:	return "Length Required";
	    case 412:	return "Precondition Failed";
	    case 413:	return "Request Entity Too Large";
	    case 414:	return "Request-URI Too Large";
	    case 415:	return "Unsupported Media Type";
	    case 500:	return "Server Error";
	    case 501:	return "Not Implemented";
	    case 502:	return "Bad Gateway";
	    case 503:	return "Service Unavailable";
	    case 504:	return "Gateway Time-out";
	    case 505:	return "HTTP Version not supported";
	    default:	return "Error";
	}
    }
}
