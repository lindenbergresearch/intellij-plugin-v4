package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
import org.antlr.intellij.plugin.Utils;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.UIHelper;
import org.antlr.v4.runtime.tree.Tree;

import java.awt.*;

import static java.lang.Math.max;
import static org.antlr.intellij.plugin.preview.ui.DefaultStyles.*;

/**
 * Provides the bounds of a specific tree-node type for layout and
 * later rendering.
 */
public class VariableExtentProvider implements NodeExtentProvider<Tree> {
    // bigger bounds for root-node
    public static double EXTENDED_BOUNDS_WIDTH = 1.15;
    public static double EXTENDED_BOUNDS_HEIGHT = 1.75;
    
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
        var s = viewer.getText(tree).split(System.lineSeparator());
        Dimension bounds;
        
        // if string consists of two lines, compute the biggest
        if (s.length > 1 && s[0].length() > 0 && s[1].length() > 0) {
            var bounds1 = UIHelper.getFullStringBounds(
                (Graphics2D) viewer.getGraphics(),
                s[0],
                BOLD_FONT
            );
            
            var bounds2 = UIHelper.getFullStringBounds(
                (Graphics2D) viewer.getGraphics(),
                s[1],
                SMALL_FONT
            );
            
            if (bounds1.getWidth() > bounds2.getWidth())
                bounds = bounds1;
            else
                bounds = bounds2;
        }
        // if not just compute the bounds of the whole string
        else {
            bounds = UIHelper.getFullStringBounds(
                (Graphics2D) viewer.getGraphics(),
                Utils.getLongestString(s),
                REGULAR_FONT
            );
        }
        
        var sem = DefaultStyles.DEFAULT_TEXT_MARGIN;
        
        if (viewer.isRootNode(tree))
            sem = ROOT_NODE_MARGIN;
        
        else if (viewer.isEOFNode(tree))
            sem = EOF_NODE_MARGIN;
        
        else if (viewer.isReSyncedNode(tree))
            sem = RESYNC_NODE_MARGIN;
        
        else if (viewer.isTerminalNode(tree))
            sem = TERMINAL_NODE_MARGIN;
        
        var w = bounds.getWidth() +
            sem.getHorizontal();
        
        //return max(w, min(viewer.minCellWidth, viewer.getMaximumTextWith()));
        return max(w, s.length == 1 ? viewer.minCellWidth / 2.f : viewer.minCellWidth);
    }
    
    
    /**
     * Provides the height of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Height in pixels.
     */
    @Override
    public double getHeight(Tree tree) {
        var s = viewer.getText(tree);
        var lines = s.split(System.lineSeparator());
        var bounds = UIHelper.getFullStringBounds(
            (Graphics2D) viewer.getGraphics(),
            s,
            REGULAR_FONT
        );
        
        var sem = DefaultStyles.DEFAULT_TEXT_MARGIN;
        
        if (viewer.isRootNode(tree))
            sem = ROOT_NODE_MARGIN;
        
        else if (viewer.isEOFNode(tree))
            sem = EOF_NODE_MARGIN;
        
        else if (viewer.isReSyncedNode(tree))
            sem = RESYNC_NODE_MARGIN;
        
        else if (viewer.isTerminalNode(tree))
            sem = TERMINAL_NODE_MARGIN;
        
        var h = bounds.getHeight() +
            sem.getVertical();
        
        return h + (lines.length - 1) * bounds.getHeight();
    }
    
    
}
