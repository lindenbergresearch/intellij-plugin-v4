package org.antlr.intellij.plugin.preview.ui.treenodes;


import com.intellij.ui.JBColor;
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
        styleProperties =
            DefaultStyles.DEFAULT_STYLE;
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
        shape = new StyledRoundRect(
            this,
            DefaultStyles.ROUND_RECT_WIDTH,
            DefaultStyles.ROUND_RECT_HEIGHT
        );
        
        add(shape);
        
        label = new StyledLabel(this);
        add(label);
        
        footer = new StyledLabel(this);
        add(footer);
        footer.setFontScale(0.75f);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize()));
    }
    
    
    /**
     * Adapt style properties for compact mode.
     */
    protected void setCompact() {
        shape.setEnabled(false);
        label.setTextColor((JBColor) shape.getBackground().brighter());
        label.setFont(label.getFont().deriveFont(label.getFont().getSize() - 3.f));
    }
    
    
    /**
     * Adapt style properties for selected tree-node.
     */
    protected void setSelected() {
        shape.setStroke(DefaultStyles.THICK_STROKE);
        shape.setBackground((JBColor) shape.getBackground().brighter());
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
