package org.antlr.intellij.plugin.preview.ui.treenodes;


import org.antlr.intellij.plugin.preview.ui.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Basic styled tree-node element.
 */
public class BasicStyledTreeNode extends StyledTreeNode {


    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public BasicStyledTreeNode() {
        super();
        styleProperties = DefaultStyles.DEFAULT_STYLE;
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public BasicStyledTreeNode(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        super(parent, viewport, styles);
    }


    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {
        shape = new StyledRoundRect(this, DefaultStyles.ROUND_RECT_WIDTH, DefaultStyles.ROUND_RECT_HEIGHT);
        add(shape);

        label = new StyledLabel(this);
        add(label);
    }


    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public void draw(Graphics2D graphics2D) {

    }
}
