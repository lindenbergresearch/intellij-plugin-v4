package org.antlr.intellij.plugin;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.misc.Tuple2;

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
     * Compares two given {@link Color} instances and a {@link JBColor} instance.
     *
     * @param color1  First color (light theme).
     * @param color2  Second color (dark theme).
     * @param jbColor JB Dual color.
     * @return Returns true if the colors are matching.
     */
    static public boolean compareColors(Color color1, Color color2, JBColor jbColor) {
        var dec = deconstructJBColor(jbColor);
        return dec.first().equals(color1) && dec.second().equals(color2);
    }
    
    
    /**
     * Compares two given {@link Color} instances wraped in a {@link Tuple2} and a {@link JBColor} instance.
     *
     * @param colors  {@link Tuple2<Color, Color>} Tuple holding the color instances.
     * @param jbColor JB Dual color.
     * @return Returns true if the colors are matching.
     */
    static public boolean compareColors(Tuple2<Color, Color> colors, JBColor jbColor) {
        var dec = deconstructJBColor(jbColor);
        
        return dec.first().equals(colors.first()) && dec.second().equals(colors.second());
        
    }
    
    
    /**
     * Deconstruct JBColor into new {@link Color} instances for bright and dark variant.
     * This is a bit hacky because if you run the IDE in dark-mode (Darcula) it
     * always returns the dark version of the color.
     * So temporary it has to be turned off.
     *
     * @param jbColor The {@link JBColor} to deconstruct.
     * @return Two new instances of {@link Color}
     */
    static public Tuple2<Color, Color> deconstructJBColor(JBColor jbColor) {
        var isDark = !JBColor.isBright();
        JBColor.setDark(false);
        
        var colorTuple = new Tuple2<>(
            new Color(jbColor.getRGB()),
            new Color(jbColor.getDarkVariant().getRGB())
        );
        
        JBColor.setDark(isDark);
        
        return colorTuple;
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
        
        var colorTuple = deconstructJBColor(color);
        return toHexColor(colorTuple.first()) + ';' + toHexColor(colorTuple.second());
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
    public static JBColor hexToJBColor(String colorHex) throws NumberFormatException {
        final var regex = "(#[0-9a-fA-F]{6})\\s*;\\s*(#[0-9a-fA-F]{6})";
        final var pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final var matcher = pattern.matcher(colorHex);
        
        // check if we have 2 colors in the correct format
        if (!matcher.matches() || matcher.groupCount() != 2) {
            throw new NumberFormatException("No valid dual hex-color string given: '" + colorHex + '\'');
        }
        
        return new JBColor(
            hexToColor(matcher.group(1)),
            hexToColor(matcher.group(2))
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
        
        var longest = s[0];
        
        for (var str : s) {
            if (str.length() > longest.length())
                longest = str;
        }
        
        return longest;
    }
    
}
