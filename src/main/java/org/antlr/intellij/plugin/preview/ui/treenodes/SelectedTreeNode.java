package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class SelectedTreeNode extends BasicStyledTreeNode {

    public SelectedTreeNode() {
        styleProperties = DefaultStyles.ERROR_NODE_STYLE;
    }


    public SelectedTreeNode(StyledElement parent, Rectangle2D viewport) {
        super(parent, viewport, DefaultStyles.SELECTED_NODE_STYLE);
    }
}
