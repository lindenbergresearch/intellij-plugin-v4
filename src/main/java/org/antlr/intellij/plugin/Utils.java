package org.antlr.intellij.plugin;

import com.intellij.ui.JBColor;

import java.awt.*;
import java.util.regex.Pattern;

public class Utils {
    
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
     * Converts a given Color to a hex string in the format: #RRGGBB
     *
     * @param color The color for encoding.
     * @return Encoded hex-string.
     */
    static public String toHexJBColor(JBColor color) {
        if (color == null)
            return "#000000;#000000";
        
        return toHexColor(color) + ';' + toHexColor(color.getDarkVariant());
    }
    
    
    /**
     * Converts a given hex string to a standard Color.
     *
     * @param colorHex Hex-string: #RRGGBB
     * @return Decoded color.
     * @throws NumberFormatException Thrown if malformed format.
     */
    public static Color hexToColor(String colorHex) throws NumberFormatException {
        final var replace = colorHex.replace("#", "0x");
        return Color.decode(replace);
    }
    
    
    /**
     * Converts a given formatted hex string with 2 colors to a JBColor.
     *
     * @param colorHex Hex-string: #RRGGBB;#RRGGBB
     * @return Decoded color.
     * @throws NumberFormatException Thrown if malformed format.
     */
    public static JBColor hexToJBColor(String colorHex, JBColor defaultColor) throws NumberFormatException {
        final var regex = "(#[0-9a-fA-F]{6})\\s*;\\s*(#[0-9a-fA-F]{6})";
        final var pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final var matcher = pattern.matcher(colorHex);
        
        // check if we have 2 color in the correct format
        if (!matcher.find() || matcher.groupCount() != 2) {
            return defaultColor;
        }
        
        return new JBColor(
            hexToColor(matcher.group(0)),
            hexToColor(matcher.group(1))
        );
    }
    
    
    /**
     * Returns the longest string in an array of strings.
     *
     * @param s Input strings as vararg, array, list.
     * @return Longest string, null if empty.
     */
    public static String getLongestString(String... s) {
        if (s == null)
            return null;
        
        if (s.length == 1)
            return s[0];
        
        String longest = s[0];
        
        for (String str : s) {
            if (str.length() > longest.length())
                longest = str;
        }
        
        return longest;
    }
    
}
