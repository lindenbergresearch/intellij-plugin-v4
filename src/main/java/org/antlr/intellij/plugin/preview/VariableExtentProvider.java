package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.UIHelper;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.*;

import static java.lang.Math.max;

/**
 * Provides the dimension of a styled tree-node based on the text properties.
 */
public class VariableExtentProvider implements NodeExtentProvider<Tree> {
    /**
     * Reference to tree-viewer.
     */
    private final UberTreeViewer viewer;
    
    
    /**
     * @param viewer
     */
    public VariableExtentProvider(UberTreeViewer viewer) {
        this.viewer = viewer;
    }
    
    
    /**
     * @param tree &nbsp;
     * @return
     */
    @Override
    public double getWidth(Tree tree) {
        String[] s = viewer.getText(tree).split("\n");
        
        Dimension bounds = UIHelper.getFullStringBounds(
            (Graphics2D) viewer.getGraphics(),
            s[s.length > 1 && s[1].length() > s[0].length() ? 1 : 0],
            DefaultStyles.REGULAR_FONT
        );
        
        double w =
            bounds.getWidth() +
            DefaultStyles.DEFAULT_TEXT_MARGIN.getHorizonal();
        
        // Do not use min size for terminals.
//        if (tree instanceof TerminalNode) {
//            return w * 1;
//        }

//        return max(w, min(viewer.minCellWidth, viewer.getMaximumTextWith()));
        return max(w, viewer.minCellWidth);
    }
    
    
    /**
     * @param tree &nbsp;
     * @return
     */
    @Override
    public double getHeight(Tree tree) {
        String s = viewer.getText(tree);
        String[] lines = s.split("\n");
        Dimension bounds = UIHelper.getFullStringBounds(
            (Graphics2D) viewer.getGraphics(),
            s,
            DefaultStyles.REGULAR_FONT
        );
        
        double h = bounds.getHeight() +
                   DefaultStyles.DEFAULT_TEXT_MARGIN.getVertical();
        
        return h + (lines.length - 1) * bounds.getHeight();
    }
    
    
}
