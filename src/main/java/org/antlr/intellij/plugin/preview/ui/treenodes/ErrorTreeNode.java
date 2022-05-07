package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class ErrorTreeNode extends BasicStyledTreeNode {
    
    public ErrorTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected) {
        super(parent, viewport, DefaultStyles.ERROR_NODE_STYLE, selected);
    }
}
