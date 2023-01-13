package org.antlr.intellij.plugin.preview.ui;


import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Staled rectangle element.
 */
public class StyledRectangle extends StyledShape {
    
    /* ----- CONSTRUCTOR -----------------------------------------------------------------------------*/
    
    
    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledRectangle(StyledElement parent) {
        super();
        this.parent = parent;
    }
    
    
    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledRectangle(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        super(parent, viewport, styles);
    }
    
    /* ----- RENDERING / EVENTS ----------------------------------------------------------------------*/
    
    
    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public void draw(Graphics2D graphics2D) {
        graphics2D.setColor(getBackground());
        graphics2D.fillRect(0, 0, (int) getWidth(), (int) getHeight());
        
        graphics2D.setColor(getForeground());
        graphics2D.drawRect(0, 0, (int) getWidth(), (int) getHeight());
    }
    
    
    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {}
}
