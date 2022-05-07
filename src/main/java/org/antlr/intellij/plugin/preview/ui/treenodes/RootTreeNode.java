package org.antlr.intellij.plugin.preview.ui.treenodes;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class RootTreeNode extends BasicStyledTreeNode {
    
    public RootTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected) {
        super(parent, viewport, DefaultStyles.ROOT_NODE_STYLE, selected);
        setOutlineColor((JBColor) getBackground().darker());
    }
}
