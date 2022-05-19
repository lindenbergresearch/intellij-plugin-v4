package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.UIHelper;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.*;

import static java.lang.Math.max;

/**
 * Provides the bounds of a specific tree-node type for layout and
 * later rendering.
 */
public class VariableExtentProvider implements NodeExtentProvider<Tree> {
    // bigger bounds for root-node and EOF
    public static double EXTENDED_BOUNDS = 1.3;
    
    /**
     * Reference to tree-viewer component.
     */
    private final UberTreeViewer viewer;
    
    
    /**
     * Creates a new VariableExtentProvider.
     *
     * @param viewer UberTreeViewer component.
     */
    public VariableExtentProvider(UberTreeViewer viewer) {
        this.viewer = viewer;
    }
    
    
    /**
     * Provides the width of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Width in pixel.
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
        if (viewer.isRootNode(tree) || viewer.isEOFNode(tree)) {
            return w * EXTENDED_BOUNDS;
        }

//        return max(w, min(viewer.minCellWidth, viewer.getMaximumTextWith()));
        return max(w, viewer.minCellWidth);
    }
    
    
    /**
     * Provides the height of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Height in pixels.
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
        
        if (viewer.isRootNode(tree) || viewer.isEOFNode(tree)) {
            return h * EXTENDED_BOUNDS;
        }
        
        return h + (lines.length - 1) * bounds.getHeight();
    }
    
    
}
