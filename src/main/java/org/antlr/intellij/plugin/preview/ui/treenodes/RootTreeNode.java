package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class RootTreeNode extends BasicStyledTreeNode {
    
    public RootTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected, boolean compact) {
        super(parent, viewport, DefaultStyles.getDefaultRootStyle(), selected, compact);
    }
}
