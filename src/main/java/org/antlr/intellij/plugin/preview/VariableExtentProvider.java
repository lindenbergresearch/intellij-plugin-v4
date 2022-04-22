package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
import org.antlr.intellij.plugin.preview.ui.StyledTreeNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.*;
import java.util.Map;

/**
 *
 */
public class VariableExtentProvider implements NodeExtentProvider<Tree> {
    private final UberTreeViewer viewer;

    Map<Tree, StyledTreeNode> cache;


    public VariableExtentProvider(UberTreeViewer viewer) {
        this.viewer = viewer;
    }


    @Override
    public double getWidth(Tree tree) {
        FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
        String s = viewer.getText(tree);

        double w = fontMetrics.stringWidth(s) +
            viewer.nodeWidthPadding *
                (viewer.isRoot(tree) ? 3. : 2.);

        // Do not use min size for terminals.
        if (tree instanceof TerminalNode) {
            return w * 1.13; //150% margin
        }

        return Math.max(w, viewer.minCellWidth);
    }


    @Override
    public double getHeight(Tree tree) {
        FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);

        double h = fontMetrics.getHeight() +
            viewer.nodeHeightPadding *
                (viewer.isRoot(tree) ? 3. : 2.);

        String s = viewer.getText(tree);
        String[] lines = s.split("\n");

        return h + (lines.length - 1) * fontMetrics.getHeight();
    }


}
