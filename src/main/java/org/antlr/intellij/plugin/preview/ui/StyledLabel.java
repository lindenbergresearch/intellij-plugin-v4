package org.antlr.intellij.plugin.preview.ui;

import java.awt.geom.Rectangle2D;

/**
 *
 */
public class StyledLabel extends StyledText {

    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledLabel() {
        super("");
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     * @param text     Label text to display.
     */
    public StyledLabel(StyledElement parent, Rectangle2D viewport, StyleProperties styles, String text) {
        super(parent, viewport, styles, text);
    }


    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {
        horizontalTextLayout = DefaultStyles.HORIZONTAL_TEXT_LAYOUT;
        verticalTextLayout = DefaultStyles.VERTICAL_TEXT_LAYOUT;
    }
}
