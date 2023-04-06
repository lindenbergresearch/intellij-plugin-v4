package org.antlr.intellij.plugin.preview.ui;


import java.awt.geom.Rectangle2D;

/**
 * Abstract shape with styled elements.
 */
public abstract class StyledShape extends StyledElement {
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
}
