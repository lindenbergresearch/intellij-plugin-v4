package org.antlr.intellij.plugin.preview.ui.treenodes;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyleProperties;
import org.antlr.intellij.plugin.preview.ui.StyledElement;

import java.awt.geom.Rectangle2D;

/**
 * Selected tree-node.
 */
public class SelectedTreeNode extends BasicStyledTreeNode {


    public SelectedTreeNode(StyledElement parent, Rectangle2D viewport, StyleProperties base) {
        super(parent, viewport, DefaultStyles.SELECTED_NODE_STYLE, true);
        this.setStyleProperties(base);

        styleProperties.setStroke(DefaultStyles.THICK_STROKE);
        setOutlineColor((JBColor) (shape.hasOutlineColor() ? getOutlineColor() : getForeground()).brighter());
        setBackground((JBColor) getBackground().brighter());
    }
}
