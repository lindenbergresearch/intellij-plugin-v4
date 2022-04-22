package org.antlr.intellij.plugin.preview.ui;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Utility methods / functions / constants.
 */
public class UIHelper {

    /**
     * Returns the "full" bounds of a given string including the descent.
     *
     * @param graphics2D Graphics context.
     * @param s          String to measure.
     * @return Bounds as double dimension.
     */
    public static Dimension getFullStringBounds(Graphics2D graphics2D, String s) {
        FontMetrics fm = graphics2D.getFontMetrics(graphics2D.getFont());
        Dimension bounds = new Dimension();

        bounds.setSize(
            fm.stringWidth(s),                  // width
            fm.getAscent() + fm.getDescent()    // height = ascending plus descending
        );

        return bounds;
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
            center.y + fm.getAscent() / 2. - fm.getDescent()/2.
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

}
