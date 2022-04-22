package org.antlr.intellij.plugin.preview.ui;

import org.antlr.v4.runtime.tree.Tree;

import java.awt.geom.Rectangle2D;

/**
 *
 */
public abstract class StyledTreeNode extends StyledElement {
    protected StyledText label;
    protected StyledShape shape;
    protected Tree node;


    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledTreeNode() {
        super();
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledTreeNode(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        super(parent, viewport, styles);
    }


    public String getText() {
        return label.text;
    }


    public void setText(String text) {
        label.text = text;
    }


    public StyledText getLabel() {
        return label;
    }


    public void setLabel(StyledText label) {
        this.label = label;
    }


    public StyledShape getShape() {
        return shape;
    }


    public void setShape(StyledShape shape) {
        this.shape = shape;
    }
}
