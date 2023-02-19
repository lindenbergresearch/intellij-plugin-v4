package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class TerminalTreeNode extends BasicStyledTreeNode {
    public static final float OFFSET = 0f;
    
    
    public TerminalTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected, boolean compact) {
        super(parent, viewport, DefaultStyles.getTerminalNodeStyle(), selected, compact);
        shiftViewport(0, OFFSET);
        shape.setFilled(false);
        
        if (selected) {
            shape.setOutlineColor(DefaultStyles.getSelectedNodeStyle().getBackground());
            shape.setStroke(DefaultStyles.THICK_STROKE);
        } else {
            shape.setOutlineColor(DefaultStyles.getTerminalNodeStyle().getBackground());
            shape.setStroke(DefaultStyles.THIN_STROKE);
        }
        
    }
    
    
}
