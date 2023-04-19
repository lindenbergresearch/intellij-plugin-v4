package org.antlr.intellij.plugin.preview.ui.treenodes;


import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.StyleProperties;
import org.antlr.intellij.plugin.preview.ui.StyledElement;
import org.antlr.intellij.plugin.preview.ui.StyledLabel;
import org.antlr.intellij.plugin.preview.ui.StyledRoundRect;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.antlr.intellij.plugin.preview.ui.DefaultStyles.*;

/**
 * Basic styled tree-node element.
 */
public class BasicStyledTreeNode extends StyledTreeNode {
    
    
    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public BasicStyledTreeNode() {
        super();
        styleProperties = DEFAULT_STYLE;
    }
    
    
    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     * @param selected Indicates node selection.
     * @param compact  Indicates if compact mode is set for this node.
     */
    public BasicStyledTreeNode(StyledElement parent, Rectangle2D viewport, StyleProperties styles, boolean selected, boolean compact) {
        super(parent, viewport, StyleProperties.deriveFrom(styles));
        
        this.selected = selected;
        this.compact = compact;
        
        if (selected)
            setSelected();
        
        if (compact)
            setCompact();
    }
    
    
    /**
     * Should be called to reset/init styles of the component.
     */
    @Override
    public void setup() {
        shape = new StyledRoundRect(this);
        
        add(shape);
        
        header = new StyledLabel(this);
        header.setTextFont(bold(header.getTextFont()));
        add(header);
        
        footer = new StyledLabel(this);
        footer.setLabel(true);
        
        add(footer);
    }
    
    
    /**
     * Adapt style properties for compact mode.
     */
    protected void setCompact() {
        if (selected)
            shape.setFilled(false);
        else
            shape.setEnabled(false);
        
        header.setTextColor((JBColor) shape.getBackground().brighter());
        header.setTextFont(header.getTextFont().deriveFont((float) header.getTextFont().getSize()));
        footer.setLabelColor(header.getTextColor());
    }
    
    
    /**
     * Adapt style properties for selected tree-node.
     */
    protected void setSelected() {
        
        if (isFilled()) {
            shape.setStroke(THICK_STROKE);
            shape.setBackground((JBColor) shape.getBackground().brighter());
        } else {
            shape.setStroke(THICK_STROKE);
            shape.setOutlineColor((JBColor) shape.getBackground().brighter());
        }
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
