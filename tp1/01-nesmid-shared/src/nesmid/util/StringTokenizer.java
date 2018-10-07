package nesmid.util;


public class StringTokenizer {
     public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
     public static final char MIN_HIGH_SURROGATE = '\uD800';
     public static final char MAX_LOW_SURROGATE  = '\uDFFF';
     public static final char MAX_HIGH_SURROGATE = '\uDBFF';
     public static final char MIN_LOW_SURROGATE  = '\uDC00';
     
     private int currentPosition;
     private int newPosition;
     private int maxPosition;
     private String str;
     private String delimiters;
     private boolean retDelims;
     private boolean delimsChanged;
     private int maxDelimCodePoint;
     private boolean hasSurrogates = false;
     private int[] delimiterCodePoints;

     private void setMaxDelimCodePoint() {
          if (delimiters == null) {
               maxDelimCodePoint = 0;
               return;
          }

          int m = 0;
          int c;
          int count = 0;
          for (int i = 0; i < delimiters.length(); i += charCount(c)) {
               c = delimiters.charAt(i);
               if (c >= MIN_HIGH_SURROGATE &&
                    c <= MAX_LOW_SURROGATE) {
                    c = codePointAt(delimiters, i);
                    hasSurrogates = true;
               }
               if (m < c)
                    m = c;
               count++;
          }
          maxDelimCodePoint = m;

          if (hasSurrogates) {
               delimiterCodePoints = new int[count];
               for (int i = 0, j = 0; i < count; i++, j += charCount(c)) {
                    c = codePointAt(delimiters, j);
                    delimiterCodePoints[i] = c;
               }
          }
     }

     public StringTokenizer(String str, String delim, boolean returnDelims) {
          currentPosition = 0;
          newPosition = -1;
          delimsChanged = false;
          this.str = str;
          maxPosition = str.length();
          delimiters = delim;
          retDelims = returnDelims;
          setMaxDelimCodePoint();
     }

     public StringTokenizer(String str, String delim) {
          this(str, delim, false);
     }

     public StringTokenizer(String str) {
          this(str, " \t\n\r\f", false);
     }

     private int skipDelimiters(int startPos) {
          if (delimiters == null)
               throw new NullPointerException();

          int position = startPos;
          while (!retDelims && position < maxPosition) {
               if (!hasSurrogates) {
                    char c = str.charAt(position);
                    if ((c > maxDelimCodePoint) || (delimiters.indexOf(c) < 0))
                         break;
                    position++;
               } else {
                    int c = codePointAt(str, position);
                    if ((c > maxDelimCodePoint) || !isDelimiter(c)) {
                         break;
                    }
                    position += charCount(c);
               }
          }
          return position;
     }

     private int scanToken(int startPos) {
          int position = startPos;
          while (position < maxPosition) {
               if (!hasSurrogates) {
                    char c = str.charAt(position);
                    if ((c <= maxDelimCodePoint) && (delimiters.indexOf(c) >= 0))
                         break;
                    position++;
               } else {
                    int c = codePointAt(str, position);
                    if ((c <= maxDelimCodePoint) && isDelimiter(c))
                         break;
                    position += charCount(c);
               }
          }
          if (retDelims && (startPos == position)) {
               if (!hasSurrogates) {
                    char c = str.charAt(position);
                    if ((c <= maxDelimCodePoint) && (delimiters.indexOf(c) >= 0))
                         position++;
               } else {
                    int c = codePointAt(str, position);
                    if ((c <= maxDelimCodePoint) && isDelimiter(c))
                         position += charCount(c);
               }
          }
          return position;
     }

     private boolean isDelimiter(int codePoint) {
          for (int i = 0; i < delimiterCodePoints.length; i++) {
               if (delimiterCodePoints[i] == codePoint) {
                    return true;
               }
          }
          return false;
     }

     public boolean hasMoreTokens() {
          newPosition = skipDelimiters(currentPosition);
          return (newPosition < maxPosition);
     }

     public String nextToken() {
          currentPosition = (newPosition >= 0 && !delimsChanged) ?
                                newPosition : skipDelimiters(currentPosition);

          delimsChanged = false;
          newPosition = -1;

          if (currentPosition >= maxPosition)
               return null; // throw new NoSuchElementException();
          int start = currentPosition;
          currentPosition = scanToken(currentPosition);
          return str.substring(start, currentPosition);
     }

     public String nextToken(String delim) {
          delimiters = delim;
          delimsChanged = true;
          setMaxDelimCodePoint();
          return nextToken();
     }

     public boolean hasMoreElements() {
          return hasMoreTokens();
     }

     public Object nextElement() {
          return nextToken();
     }

     public int countTokens() {
          int count = 0;
          int currpos = currentPosition;
          while (currpos < maxPosition) {
               currpos = skipDelimiters(currpos);
               if (currpos >= maxPosition)
                    break;
               currpos = scanToken(currpos);
               count++;
          }
          return count;
     }
     
     private int charCount(int codePoint) {
          return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT? 2 : 1;
     }
     
     private int codePointAt(String seq, int index) {
          char c1 = seq.charAt(index++);
          if (isHighSurrogate(c1)) {
               if (index < seq.length()) {
                    char c2 = seq.charAt(index);
                    if (isLowSurrogate(c2)) {
                         return toCodePoint(c1, c2);
                    }
               }
          }
          return c1;
     }
     
     private boolean isHighSurrogate(char ch) {
          return ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE;
     }
     
     private boolean isLowSurrogate(char ch) {
          return ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE;
     }
     
     private int toCodePoint(char high, char low) {
          return ((high - MIN_HIGH_SURROGATE) << 10)
               + (low - MIN_LOW_SURROGATE) + MIN_SUPPLEMENTARY_CODE_POINT;
     }
}