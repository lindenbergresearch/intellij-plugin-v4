package org.antlr.intellij.plugin;

import java.awt.*;

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
