package org.antlr.intellij.plugin.preview.ui;

import java.awt.*;
import java.awt.geom.Rectangle2D;


/**
 *
 */
public class BasicStyledElement extends StyledElement {

    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public BasicStyledElement() {
        super();
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public BasicStyledElement(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        super(parent, viewport, styles);
    }


    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {
        styleProperties = DefaultStyles.DEFAULT_STYLE;
    }


    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public void draw(Graphics2D graphics2D) {}
}
