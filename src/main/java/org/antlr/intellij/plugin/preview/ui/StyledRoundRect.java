package org.antlr.intellij.plugin.preview.ui;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Round rect shape with styling attributes.
 */
public class StyledRoundRect extends StyledShape {
    
    protected int arcWidth;
    protected int arcHeight;
    
    
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
        if (isFilled()) {
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, (int) getWidth(), (int) getHeight(), getArcWidth(), getArcHeight());
        }
        
        graphics2D.setColor(getOutlineColor());
        graphics2D.drawRoundRect(0, 0, (int) getWidth(), (int) getHeight(), getArcWidth(), getArcHeight());
    }
    
    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/
    
    
    public int getArcWidth() {
        return styleProperties.getArcDiameter();
    }
    
    
    public int getArcHeight() {
        return styleProperties.getArcDiameter();
    }
}
