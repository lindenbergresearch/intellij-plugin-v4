package org.antlr.intellij.plugin.preview.ui;

import com.intellij.ui.JBColor;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Round rect shape with styling attributes.
 */
public class StyledRoundRect extends StyledShape {

    protected int arcWidth;
    protected int arcHeight;
    boolean filled = true;

    /* ----- CONSTRUCTOR -----------------------------------------------------------------------------*/


    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledRoundRect(StyledElement parent, int arcWidth, int arcHeight) {
        super();
        this.parent = parent;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledRoundRect(StyledElement parent, Rectangle2D viewport, StyleProperties styles, int arcWidth, int arcHeight) {
        super(parent, viewport, styles);
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }

    /* ----- RENDERING / EVENTS ----------------------------------------------------------------------*/


    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {}


    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public void draw(Graphics2D graphics2D) {
        if (hasOutlineColor()) {
            setForeground(getOutlineColor());
        } else {
            setForeground((JBColor) getBackground().darker());
        }

        if (filled) {
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, (int) getWidth(), (int) getHeight(), arcWidth, arcHeight);
        }

        graphics2D.setColor(getForeground());
        graphics2D.drawRoundRect(0, 0, (int) getWidth(), (int) getHeight(), arcWidth, arcHeight);
    }

    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/


    public int getArcWidth() {
        return arcWidth;
    }


    public void setArcWidth(int arcWidth) {
        this.arcWidth = arcWidth;
    }


    public int getArcHeight() {
        return arcHeight;
    }


    public void setArcHeight(int arcHeight) {
        this.arcHeight = arcHeight;
    }


    public boolean isFilled() {
        return filled;
    }


    public void setFilled(boolean filled) {
        this.filled = filled;
    }
}
