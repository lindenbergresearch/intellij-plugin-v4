package org.antlr.intellij.plugin.preview.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.JBFont;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Utility methods / functions / constants.
 */
public class UIHelper {
    
    public static final Logger LOG =
        Logger.getInstance("ANTLR UIHelper");
    
    
    /**
     * Returns the "full" bounds of a given string including the descent.
     *
     * @param graphics2D Graphics context.
     * @param s          String to measure.
     * @param font       Font to measure.
     * @return Bounds as double dimension.
     */
    public static Dimension getFullStringBounds(Graphics2D graphics2D, String s, Font font) {
        
        if (graphics2D == null) return new Dimension(0, 0);
        
        var fm = graphics2D.getFontMetrics(font);
        var bounds = new Dimension();
        
        bounds.setSize(
            fm.stringWidth(s),                  // width
            fm.getAscent() /*+ fm.getDescent()*/    // height = ascending plus descending
        );
        
        return bounds;
    }
    
    
    /**
     * Returns the "full" bounds of a given string including the descent.
     * Using the current font set at graphics-context.
     *
     * @param graphics2D Graphics context.
     * @param s          String to measure.
     * @return Bounds as double dimension.
     */
    public static Dimension getFullStringBounds(Graphics2D graphics2D, String s) {
        return getFullStringBounds(graphics2D, s, graphics2D.getFont());
    }
    
    
    /**
     * Computes the origin of a string based on a given point defining the center.
     *
     * @param graphics2D Graphics context.
     * @param center     Point defining the center.
     * @param s          String to draw.
     */
    public static Point2D.Double getStringCentered(Graphics2D graphics2D, Point2D.Double center, String s) {
        FontMetrics fm = graphics2D.getFontMetrics(graphics2D.getFont());
        Dimension bounds = getFullStringBounds(graphics2D, s);
        
        return new Point2D.Double(
            center.x - bounds.getWidth() / 2.,
            center.y + fm.getAscent() / 2. - fm.getDescent() / 2.
        );
    }
    
    
    /**
     * Draws a string based on a given point defining the center.
     *
     * @param graphics2D Graphics context.
     * @param center     Point defining the center.
     * @param s          String to draw.
     */
    public static void drawStringCentered(Graphics2D graphics2D, Point2D.Double center, String s) {
        Point2D.Double origin = getStringCentered(graphics2D, center, s);
        graphics2D.drawString(s, (float) origin.x, (float) origin.y);
    }
    
    
    /**
     * Load font from jar-file.
     *
     * @param name  Name of the font to load without '.ttf' suffix, located at: 'resources/fonts'.
     * @param clazz The class pointing to the jar-file.
     * @return New font object or default font.
     */
    public static Font createTrueType(String name, Object clazz) {
        var fileName = "/fonts/" + name + ".ttf";
        var is = clazz.getClass().getResourceAsStream(fileName);
        
        if (is != null) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, is);
            } catch (Exception e) {
                LOG.warn("Unable to load font from resources: " + fileName);
                
                // fallback to regular
                return JBFont.regular();
            }
        }
        
        return JBFont.regular();
    }
}
