package org.antlr.intellij.plugin.preview.ui.treenodes;

import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;
import org.antlr.intellij.plugin.preview.ui.StyledRoundRect;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TerminalTreeNode extends BasicStyledTreeNode {
    public static final float OFFSET = -10f;

    // use small labels
    boolean smallLabel;


    public TerminalTreeNode() {
        styleProperties = DefaultStyles.ERROR_NODE_STYLE;
    }


    public TerminalTreeNode(StyledElement parent, Rectangle2D viewport) {
        super(parent, viewport, DefaultStyles.TERMINAL_NODE_STYLE);
        shiftViewport(0, OFFSET);

        if (shape instanceof StyledRoundRect) {
            ((StyledRoundRect) shape).setArc(new Dimension(20, 20));
        }
    }


    public boolean isSmallLabel() {
        return smallLabel;
    }


    public void setSmallLabel(boolean smallLabel) {
        this.smallLabel = smallLabel;

        if (smallLabel)
            setFont(DefaultStyles.SMALL_TERMINAL_FONT);
        else
            setFont(DefaultStyles.TERMINAL_FONT);
    }
}
