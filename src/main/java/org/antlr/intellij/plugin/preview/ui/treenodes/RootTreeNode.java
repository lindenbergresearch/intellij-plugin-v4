package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class RootTreeNode extends BasicStyledTreeNode {


    public RootTreeNode() {
        styleProperties = DefaultStyles.ROOT_NODE_STYLE;
    }


    public RootTreeNode(StyledElement parent, Rectangle2D viewport) {
        super(parent, viewport, DefaultStyles.ROOT_NODE_STYLE);
    }
}
