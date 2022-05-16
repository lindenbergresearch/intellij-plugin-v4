package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class EOFTreeNode extends BasicStyledTreeNode {
    
    public EOFTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected, boolean compact) {
        super(parent, viewport, DefaultStyles.EOF_NODE_STYLE, selected,compact);
    }
}
