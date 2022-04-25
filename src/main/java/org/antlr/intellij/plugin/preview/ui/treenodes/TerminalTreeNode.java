package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class TerminalTreeNode extends BasicStyledTreeNode {

    public TerminalTreeNode() {
        styleProperties = DefaultStyles.ERROR_NODE_STYLE;
    }


    public TerminalTreeNode(StyledElement parent, Rectangle2D viewport) {
        super(parent, viewport, DefaultStyles.TERMINAL_NODE_STYLE);
    }
}
