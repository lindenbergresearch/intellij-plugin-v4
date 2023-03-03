package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
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
        var lines = viewer.getText(tree).trim().split(System.lineSeparator());
        Dimension bounds;
        
        // if string consists of two lines, compute the biggest
        if (lines.length > 1 && lines[0].length() > 0 && lines[1].length() > 0) {
            var boundsTitle = UIHelper.getFullStringBounds(
                viewer.getGraphics2D(),
                lines[0],
                BASIC_FONT
            );
            
            var boundsLabel = UIHelper.getFullStringBounds(
                viewer.getGraphics2D(),
                lines[1],
                LABEL_FONT
            );
            
            if (boundsTitle.getWidth() > boundsLabel.getWidth())
                bounds = boundsTitle;
            else
                bounds = boundsLabel;
        }
        // if not just compute the bounds of the whole string
        else {
            bounds = UIHelper.getFullStringBounds(
                viewer.getGraphics2D(),
                lines[0],
                BASIC_FONT
            );
        }
        
        var margin = DefaultStyles.DEFAULT_TEXT_MARGIN;
        
        if (viewer.isRootNode(tree))
            margin = ROOT_NODE_MARGIN;
        
        else if (viewer.isEOFNode(tree))
            margin = EOF_NODE_MARGIN;
        
        else if (viewer.isReSyncedNode(tree))
            margin = RESYNC_NODE_MARGIN;
        
        else if (viewer.isTerminalNode(tree))
            margin = TERMINAL_NODE_MARGIN;
        
        var w = bounds.getWidth() +
            margin.getHorizontal();
        
        //return max(w, min(viewer.minCellWidth, viewer.getMaximumTextWith()));
        return max(w, lines.length == 1 ? viewer.minCellWidth / 2.f : viewer.minCellWidth);
    }
    
    
    /**
     * Provides the height of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Height in pixels.
     */
    @Override
    public double getHeight(Tree tree) {
        var text = viewer.getText(tree);
        var lines = text.trim().split(System.lineSeparator());
        var bounds = UIHelper.getFullStringBounds(
            viewer.getGraphics2D(),
            text,
            BASIC_FONT
        );
        
        var margin = DefaultStyles.DEFAULT_TEXT_MARGIN;
        
        if (viewer.isRootNode(tree))
            margin = ROOT_NODE_MARGIN;
        
        else if (viewer.isEOFNode(tree))
            margin = EOF_NODE_MARGIN;
        
        else if (viewer.isReSyncedNode(tree))
            margin = RESYNC_NODE_MARGIN;
        
        else if (viewer.isTerminalNode(tree))
            margin = TERMINAL_NODE_MARGIN;
        
        var h = bounds.getHeight() +
            margin.getVertical();
        
        return max(h + (lines.length - 1) * bounds.getHeight(), viewer.minCellHeight);
    }
    
    
}
