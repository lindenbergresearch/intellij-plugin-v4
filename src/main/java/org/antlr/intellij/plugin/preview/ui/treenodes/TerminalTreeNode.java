package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

public class TerminalTreeNode extends BasicStyledTreeNode {
    public static final float OFFSET = 0f;
    
    
    public TerminalTreeNode(StyledElement parent, Rectangle2D viewport, boolean selected, boolean compact) {
        super(parent, viewport, DefaultStyles.getTerminalNodeStyle(), selected, compact);
        shiftViewport(0, OFFSET);
    }
}
