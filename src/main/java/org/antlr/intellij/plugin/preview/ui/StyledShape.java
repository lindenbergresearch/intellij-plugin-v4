package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.JBColor;

import java.awt.geom.Rectangle2D;

/**
 * Abstract shape with styled elements.
 */
public abstract class StyledShape extends StyledElement {
    // separate outline color
    private JBColor outlineColor;
    private boolean filled = true;
    
    
    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledShape() {
        super();
    }
    
    
    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledShape(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        super(parent, viewport, styles);
    }
    
    
    public boolean hasOutlineColor() {
        return outlineColor != null;
    }
    
    
    public JBColor getOutlineColor() {
        return outlineColor;
    }
    
    
    public void setOutlineColor(JBColor outlineColor) {
        this.outlineColor = outlineColor;
    }
    
    
    public boolean isFilled() {
        return filled;
    }


    public void setFilled(boolean filled) {
        this.filled = filled;
    }
}
