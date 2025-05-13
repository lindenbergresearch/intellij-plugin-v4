package org.antlr.intellij.plugin.misc;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StringUtil {
    public enum ShortenType {
        START,
        MIDDLE,
        END
    }
    
    
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yy/MM/dd HH:mm:ss.SSS";
    public static final String SIMPLE_TIMESTAMP_FORMAT = "HH:mm:ss.SSS";
    
    
    /**
     * Converts a given Color to a hex string in the format: #RRGGBB
     *
     * @param color The color for encoding.
     * @return Encoded hex-string.
     */
    static public String toHexColor(Color color) {
        if (color == null)
            return "#000000";
        
        var strBuilder = new StringBuilder().append('#');
        var val = Long.toHexString((long) color.getRGB() & 0xFFFFFF);
        strBuilder.append("0".repeat((6 - val.length())));
        strBuilder.append(val);
        
        return strBuilder.toString();
    }
    
    
    /**
     * Creates a string with a repeated given char.
     *
     * @param c      The char to repeat.
     * @param length Length of repetition.
     * @return The resulting String.
     */
    private static String printCharLine(char c, int length) {
        var buffer = new StringBuilder();
        
        for (var i = 0; i < length; i++) {
            buffer.append(c);
        }
        
        return buffer.toString();
    }
    
    
    /**
     * Returns the longest string in an array of strings.
     *
     * @param s Input strings as vararg, array, list.
     * @return Longest string, null if empty.
     */
    public static String getLongestString(String... s) {
        if (s == null || s.length == 0) {
            return null;
        }
        
        String maxString = null;
        
        for (var str : s) {
            if (str == null) {
                continue;
            }
            if (maxString == null || str.length() > maxString.length()) {
                maxString = str;
            }
        }
        
        return maxString;
    }
    
    
    public static String getTimeStamp() {
        return new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT).format(Calendar.getInstance().getTime());
    }
    
    
    public static String getSimpleTimeStamp() {
        return new SimpleDateFormat(SIMPLE_TIMESTAMP_FORMAT).format(Calendar.getInstance().getTime());
    }
    
    
    public static String getTimeStamp(String format) {
        return new SimpleDateFormat(format).format(Calendar.getInstance().getTime());
    }
    
    
    public static String byPaddingZeros(int value, int paddingLength) {
        return String.format("%0" + paddingLength + 'd', value);
    }
    
    
    public static String padLeft(String s, int paddingLength) {
        return String.format("%-" + paddingLength + 's', s);
    }
    
    
    public static String padRight(String s, int paddingLength) {
        return String.format("%" + paddingLength + 's', s);
    }
    
    
    private final static ShortenType defaultShortenType = ShortenType.MIDDLE;
    private final static String defaultEllipsis = " … ";
    
    
    public static String shortenString(String text, int maxLength, ShortenType type, String ellipsis) {
        if (text == null || ellipsis == null || maxLength <= ellipsis.length()) {
            return text;
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        var availableLength = maxLength - ellipsis.length();
        
        switch (type) {
            case START:
                return ellipsis + text.substring(text.length() - availableLength);
            case MIDDLE:
                var startLength = availableLength / 2;
                var endLength = availableLength - startLength;
                return text.substring(0, startLength) + ellipsis + text.substring(text.length() - endLength);
            case END:
                return text.substring(0, availableLength) + ellipsis;
            default:
                throw new IllegalArgumentException("Unknown shorten type: " + type);
        }
    }
    
    
    public static String elided(String text, int maxlength) {
        return shortenString(text, maxlength, defaultShortenType, defaultEllipsis);
    }
}