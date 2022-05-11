package org.antlr.intellij.plugin.preview.ui.treenodes;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class TerminalTreeNode extends BasicStyledTreeNode {
    public static final float OFFSET = 0f;
    
    
    public TerminalTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected) {
        super(parent, viewport, DefaultStyles.TERMINAL_NODE_STYLE, selected);
        shiftViewport(0, OFFSET);
        shape.setFilled(selected);
        shape.setEnabled(selected);
        
        if (selected) {
            shape.setOutlineColor(JBColor.BLUE);
            label.setTextColor(DefaultStyles.JB_COLOR_YELLOW);
            footer.setTextColor(DefaultStyles.JB_COLOR_YELLOW);
        }
        
    }
    
    
}
