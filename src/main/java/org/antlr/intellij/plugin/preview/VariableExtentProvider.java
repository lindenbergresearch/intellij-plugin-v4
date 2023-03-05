package org.antlr.intellij.plugin.preview;

import org.abego.treelayout.NodeExtentProvider;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.DoubleDimension2D;
import org.antlr.intellij.plugin.preview.ui.UIHelper;
import org.antlr.v4.runtime.tree.ParseTree;
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
     * Controls the way each node is measured.
     */
    enum ExtentMode {
        PRECISE_BOUNDS,  // returns the exact dimension of a node
        FIXED_BOUNDS,    // returns a fixed size for all nodes
        MAXIMIZED_BOUNDS // computes the biggest node and use it for all
    }
    
    
    /**
     * Reference to tree-viewer component.
     */
    private final UberTreeViewer viewer;
    
    /** dimension holed a setup fixed size */
    private DoubleDimension2D fixedBoundsDimension = DoubleDimension2D.ZERO;
    
    /** dimension holds the max. computed size */
    private DoubleDimension2D  maxDimension = DoubleDimension2D.ZERO;
    
    /** extend provider mode */
    private ExtentMode extentMode = ExtentMode.MAXIMIZED_BOUNDS;
    
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Creates a new VariableExtentProvider.
     *
     * @param viewer UberTreeViewer component.
     */
    public VariableExtentProvider(UberTreeViewer viewer) {
        this.viewer = viewer;
    }
    
    
    public DoubleDimension2D getMaxDimension() {
        return maxDimension;
    }
    
    
    public void setMaxDimension(DoubleDimension2D maxDimension) {
        this.maxDimension = maxDimension;
    }
    
    
    public DoubleDimension2D getFixedBoundsDimension() {
        return fixedBoundsDimension;
    }
    
    
    public void setFixedBoundsDimension(DoubleDimension2D fixedBoundsDimension) {
        this.fixedBoundsDimension = fixedBoundsDimension;
    }
    
    
    public ExtentMode getExtentMode() {
        return extentMode;
    }
    
    
    public void setExtentMode(ExtentMode extentMode) {
        this.extentMode = extentMode;
    }
    
    
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Computes the width of the nodes text based on it's content.
     *
     * @param tree The tree-node to measure.
     * @return The width in pixel.
     */
    private double getWidthText(Tree tree) {
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
        
        return max(w, lines.length == 1 ? viewer.minCellWidth / 2.f : viewer.minCellWidth);
    }
    
    
    /**
     * @param tree
     * @return
     */
    private double getHeightText(Tree tree) {
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
    
    
    /**
     * Returns the maximum with in pixels needed by a tree-node, and
     * it's sub-nodes, based on the given root tree-node.
     *
     * @return The maximum dimension found in pixel.
     */
    public DoubleDimension2D computeMaxDimension() {
        if (viewer.root == null)
            return DoubleDimension2D.ZERO;
        
        return computeRecMaxDimension((ParseTree) viewer.root, DoubleDimension2D.ZERO);
    }
    
    
    /**
     * Returns the maximum with in pixels needed by a tree-node, and
     * it's sub-nodes, based on the given root tree-node.
     *
     * @param tree   The tree-node to examine.
     * @param maxDim The current maximum.
     * @return The current maximum dimension in pixel.
     */
    private DoubleDimension2D computeRecMaxDimension(ParseTree tree, DoubleDimension2D maxDim) {
        var currMaxDim = getNodeDimension(tree).max(maxDim);
        
        for (var i = 0; i < tree.getChildCount(); i++) {
            currMaxDim.max(computeRecMaxDimension(tree.getChild(i), currMaxDim));
        }
        
        return currMaxDim;
    }
    
    
    /**
     * Compute the maximum text dimensions based of the parse-tree.
     */
    public void updateMaxTextDimensions() {
        setMaxDimension(computeMaxDimension());
    }
    
    
    public void switchMode(ExtentMode extentMode) {
        this.extentMode = extentMode;
        
        if (extentMode == ExtentMode.MAXIMIZED_BOUNDS) {
            updateMaxTextDimensions();
        }
    }
    
    
    /**
     * Called upon any change of the parse-tree.
     *
     * @param root The root node of the parse-tree,
     */
    public void onUpdateParseTree(Tree root) {
        if (extentMode == ExtentMode.MAXIMIZED_BOUNDS) {
            updateMaxTextDimensions();
        }
    }
    
    
    /**
     * Provides the width of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Width in pixel.
     */
    @Override
    public double getWidth(Tree tree) {
        if (extentMode == ExtentMode.MAXIMIZED_BOUNDS && !maxDimension.eitherZero()) {
            return maxDimension.getWidth();
        }
        
        if (extentMode == ExtentMode.FIXED_BOUNDS && !fixedBoundsDimension.eitherZero()) {
            return fixedBoundsDimension.getWidth();
        }
        
        return getWidthText(tree);
    }
    
    
    /**
     * Provides the height of a specific tree-node type.
     *
     * @param tree Tree node to examine.
     * @return Height in pixels.
     */
    @Override
    public double getHeight(Tree tree) {
        if (extentMode == ExtentMode.MAXIMIZED_BOUNDS && !maxDimension.eitherZero()) {
            return maxDimension.getHeight();
        }
        
        if (extentMode == ExtentMode.FIXED_BOUNDS && !fixedBoundsDimension.eitherZero()) {
            return fixedBoundsDimension.getHeight();
        }
        
        return getHeightText(tree);
    }
    
    
    /**
     * @param tree
     * @return
     */
    public DoubleDimension2D getNodeDimension(Tree tree) {
        return new DoubleDimension2D(getWidthText(tree), getHeightText(tree));
    }
    
    
}
