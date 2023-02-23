package org.antlr.intellij.plugin.preview.ui.treenodes;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.StyleProperties;
import org.antlr.intellij.plugin.preview.ui.StyledElement;
import org.antlr.intellij.plugin.preview.ui.StyledShape;
import org.antlr.intellij.plugin.preview.ui.StyledText;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.geom.Rectangle2D;

/**
 * Abstract tree-node with style attributes.
 */
public abstract class StyledTreeNode extends StyledElement {
    protected StyledText label, footer;
    protected StyledShape shape;
    protected Tree node;
    protected String[] lines;
    protected double spacing = 0.68;
    protected boolean selected = false;
    protected boolean compact = false;
    
    
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
        return label.getText();
    }
    
    
    public void setOutlineColor(JBColor color) {
        shape.setOutlineColor(color);
    }
    
    
    public JBColor getOutlineColor() {
        return shape.getOutlineColor();
    }
    
    
    public void setText(String text) {
        lines = text.split(System.lineSeparator());
        
        if (lines.length > 1) {
            label.setText(lines[0]);
            footer.setText(lines[1]);
        } else {
            label.setText(text);
        }
        
        updateLayout();
    }
    
    
    /**
     * Set layout of either one or two labels
     * depending on the text.
     */
    protected void updateLayout() {
        if (lines.length > 1) {
            
            // the upper text box
            Rectangle2D upper =
                new Rectangle2D.Double(
                    viewport.getX(),
                    viewport.getY(),
                    viewport.getWidth(),
                    viewport.getHeight() * spacing
                
                );
            
            // the lower text box
            Rectangle2D lower =
                new Rectangle2D.Double(
                    viewport.getX(),
                    viewport.getY() + viewport.getHeight() * (1 - spacing),
                    viewport.getWidth(),
                    viewport.getHeight() * spacing
                
                );
            
            label.setViewport(upper);
            footer.setViewport(lower);
            
            return;
        }
        
        label.setViewport(viewport);
        footer.setViewport(viewport);
    }
    
    
    public StyledText getLabel() {
        return label;
    }
    
    
    public void setLabel(StyledText label) {
        this.label = label;
    }
    
    
    public StyledText getFooter() {
        return footer;
    }
    
    
    public void setFooter(StyledText footer) {
        this.footer = footer;
    }
    
    
    public double getSpacing() {
        return spacing;
    }
    
    
    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }
    
    
    public StyledShape getShape() {
        return shape;
    }
    
    
    public void setShape(StyledShape shape) {
        this.shape = shape;
    }
    
    
    public boolean isSelected() {
        return selected;
    }
    
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    
    public boolean isCompact() {
        return compact;
    }
    
    
    public void setCompact(boolean compact) {
        this.compact = compact;
    }
}
