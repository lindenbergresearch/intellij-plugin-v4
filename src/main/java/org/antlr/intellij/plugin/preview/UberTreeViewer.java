package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.abego.treelayout.Configuration.AlignmentInLevel;
import org.abego.treelayout.Configuration.Location;
import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.antlr.intellij.plugin.preview.ui.*;
import org.antlr.intellij.plugin.preview.ui.treenodes.*;
import org.antlr.v4.gui.TreeLayoutAdaptor;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.*;
import org.apache.xmlgraphics.java2d.Dimension2DDouble;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.awt.RenderingHints.*;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Custom tree layout viewer component.
 * Enhanced version based on: {@code TreeViewer}
 */
public class UberTreeViewer extends JComponent implements MouseListener, MouseMotionListener {
    private static final Logger LOG =
        Logger.getInstance("ANTLR UberTreeViewer");
    
    /*---- CONSTANTS ----------------------------------------------------------------------------*/
    // manual scaling factor interval
    public final static double MAX_SCALE_FACTOR = 2.0;
    public final static double MIN_SCALE_FACTOR = 0.1;
    
    // range for the gap between nodes
    public final static double MAX_NODES_GAP = 60;
    public final static double MIN_NODES_GAP = 5;
    public final static double NODE_GAP_INCREMENT = 5;
    public static final int DEFAULT_GAP_BETWEEN_NODES = 25;
    
    
    // auto-scale factor interval
    public final static double MAX_AUTO_SCALE_FACTOR = 2.0;
    public final static double MIN_AUTO_SCALE_FACTOR = 0.01;
    
    // scaling increment +/- used by zoom action
    public final static double SCALING_INCREMENT = 0.15;
    
    // margin to be guaranteed around a selected node
    // scaling factor used by node focus action
    public final static double NODE_FOCUS_MARGIN = 100;
    public final static double NODE_FOCUS_SCALE_FACTOR = 1.25;
    
    public final static int VIEWER_HORIZONTAL_MARGIN = 25;
    public final static int VIEWER_VERTICAL_MARGIN = 25;
    
    // margin around a selected tree-node that should
    // be not covered by the border
    public final static int SCROLL_VIEWPORT_MARGIN = 20;
    
    // shrink factors for compact mode
    public static final double COMPACT_LABELS_FACTOR_HORIZONTAL = 0.5;
    public static final double COMPACT_LABELS_FACTOR_VERTICAL = 0.3;
    
    /*---- CURSOR -------------------------------------------------------------------------------*/
    public static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    public static final Cursor SELECT_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    public static final Cursor DRAG_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    
    /*---- COLORS -------------------------------------------------------------------------------*/
    protected JBColor edgesColor;
    
    /*---- MOUSE --------------------------------------------------------------------------------*/
    private Point2D lastMousePos;
    private Point2D currentMousePos;
    private final Point2D deltaMousePos;
    
    private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();
    protected JScrollPane scrollPane;
    protected VariableExtentProvider extentProvider;
    protected Location layoutOrientation;
    protected int minCellWidth;
    protected int minCellHeight;
    protected float edgesStrokeWidth;
    protected boolean autoscaling;
    protected double scale;
    protected double renderTime;
    protected double createTime;
    protected double parseTime;
    protected int objects;
    protected boolean compactLabels;
    protected boolean showParsingInfo;
    protected boolean treeInvalidated;
    protected Point2D offset;
    protected Dimension viewport;
    protected Tree selectedTreeNode;
    protected BasicStyledElement styledRootNode;
    
    protected Font font;
    boolean useCurvedEdges;
    
    protected double gapBetweenNodes;
    
    
    protected TreeLayout<Tree> treeLayout;
    protected Tree root;
    protected TreeTextProvider treeTextProvider;
    
    public PreviewPanel previewPanel;
    protected Rectangle marginBox;
    
    protected JInfoLabel infoLabel;
    
    
    protected VirtualFile grammarFile;
    protected PreviewState previewState;
    
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Default tree-text provider.
     */
    public static class DefaultTreeTextProvider implements TreeTextProvider {
        private final List<String> ruleNames;
        
        
        public DefaultTreeTextProvider(List<String> ruleNames) {
            this.ruleNames = ruleNames;
        }
        
        
        @Override
        public String getText(Tree node) {
            return String.valueOf(Trees.getNodeText(node, ruleNames));
        }
    }
    
    
    /**
     * Tests for the given layout orientation.
     *
     * @param layoutOrientation Layout orientation.
     * @return True if matches.
     */
    public boolean hasLayoutOrientation(Location layoutOrientation) {
        return this.layoutOrientation == layoutOrientation;
    }
    
    
    /**
     * Sets the layout orientation.
     *
     * @param layoutOrientation Layout orientation.
     */
    public void setLayoutOrientation(Location layoutOrientation) {
        this.layoutOrientation = layoutOrientation;
    }
    
    
    /**
     * Creates the UberTreeViewer component based on the given tree node.
     *
     * @param previewPanel Reference to the PreviewPanel.
     */
    public UberTreeViewer(PreviewPanel previewPanel) {
        this.previewPanel = previewPanel;
        this.setBackground(DefaultStyles.getColorFromAppSettings(ColorKey.VIEWER_BACKGROUND));
        this.layoutOrientation = Location.Top;
        
        /* get instance of node bounds provider */
        extentProvider = new VariableExtentProvider(this);
        
        /* draw offset for diagram - needed to draw centered */
        offset = new Point2D.Double(0, 0);
        
        /* font setup */
        font = DefaultStyles.BOLD_FONT;
        
        /* edges setup */
        useCurvedEdges = false;
        edgesColor = JBColor.BLACK;
        edgesStrokeWidth = 1.5f;
        
        minCellWidth = 50;
        minCellHeight = 30;
        gapBetweenNodes = DEFAULT_GAP_BETWEEN_NODES;
        
        scale = 1.f;
        autoscaling = true;
        
        compactLabels = false;
        showParsingInfo = true;
        
        // add handler for mouse events
        addMouseListener(this);
        addMouseMotionListener(this);
        
        lastMousePos = new Point(0, 0);
        currentMousePos = new Point(0, 0);
        deltaMousePos = new Point(0, 0);
        
        setAutoscrolls(true);
        
        //--- setup textual info box ---//
        infoLabel = new JInfoLabel();
        this.infoLabel.setVisible(false);
        
        
        infoLabel.addLabelElement(
            "parse_time",
            new InfoLabelElement<>("Parse time", "%.3fms", 0.0)
        );
        
        infoLabel.addLabelElement(
            "render_time",
            new InfoLabelElement<>("Render time", "%.3fms", 0.0)
        );
        
        infoLabel.addLabelElement(
            "create_time",
            new InfoLabelElement<>("Tree create time", "%.3fms", 0.0)
        );
        
        infoLabel.addLabelElement(
            "num_burden",
            new InfoLabelElement<>("Lookahead burden", "%s")
        );
        
        infoLabel.addLabelElement(
            "num_ops",
            new InfoLabelElement<>("DFA cache miss rate", "%s")
        );
        
        infoLabel.addLabelElement(
            "num_objects",
            new InfoLabelElement<>("Total nodes", "%d", 0)
        );
        
        infoLabel.addLabelElement(
            "num_tokens",
            new InfoLabelElement<>("Tokens", "%d", 0)
        );
        
        infoLabel.addLabelElement(
            "prediction_time",
            new InfoLabelElement<>("Prediction time", "%s")
        );
        
        infoLabel.addLabelElement(
            "num_chars",
            new InfoLabelElement<>("total chars", "%s")
        );
    }
    
    
    /**
     * Checks if the tree-view is showing some basic parsing info.
     *
     * @return True if info is shown.
     */
    public boolean isShowParsingInfo() {
        return showParsingInfo;
    }
    
    
    /**
     * Set if the tree-viewer should show common parsing-info.
     *
     * @param showParsingInfo Bool flag, if true info is shown.
     */
    public void setShowParsingInfo(boolean showParsingInfo) {
        infoLabel.setVisible(showParsingInfo);
        this.showParsingInfo = showParsingInfo;
    }
    
    
    /**
     * Returns the info-label component.
     *
     * @return JInfoLabel
     */
    public JInfoLabel getInfoLabel() {
        return infoLabel;
    }
    
    
    /**
     * Updates all data for the parse-info labels in the tree-view.
     *
     * @param previewState PreviewState
     * @param duration     Computed time needed by parser.
     */
    public void updateParseData() {
        if (!showParsingInfo)
            return;
        
        var duration = previewState.parseTime;

//        var controller = ANTLRv4PluginController.getInstance(previewPanel.project);
//        var previewState = controller.getPreviewState()
//
        var parser = previewState.parsingResult.parser;
        var parseInfo = parser.getParseInfo();
        var predictionTimeMS = parseInfo.getTotalTimeInPrediction() / 10e6;
        
        TokenStream tokens = parser.getInputStream();
        int numTokens = tokens.size();
        Token lastToken = tokens.get(numTokens - 1);
        int numChar = lastToken.getStopIndex();
        int numLines = lastToken.getLine();
        
        if (lastToken.getType() == Token.EOF) {
            if (numTokens <= 1) {
                numLines = 0;
            } else {
                var secondToLastToken = tokens.get(numTokens - 2);
                numLines = secondToLastToken.getLine();
            }
        }
        
        double look =
            parseInfo.getTotalSLLLookaheadOps() +
                parseInfo.getTotalLLLookaheadOps();
        
        double atnLook = parseInfo.getTotalATNLookaheadOps();
        var parseTimeMS = duration / 10e6;
        
        var stat = String.format(Locale.ENGLISH, "%d char(s), %d line(s)", numChar, numLines);
        var burden = String.format(Locale.ENGLISH, "%2d/2%d %.2f", (long) look, numTokens, look / numTokens);
        var ops = String.format(Locale.ENGLISH, "%2d/2%d %.2f%%", (long) atnLook, (long) look, atnLook * 100.0 / look);
        var ptime = String.format(Locale.ENGLISH, "%.3fms %3.2f%%", predictionTimeMS, 100 * (predictionTimeMS) / parseTimeMS);
        
        infoLabel.updateElement("parse_time", parseTimeMS);
        infoLabel.updateElement("prediction_time", ptime);
        infoLabel.updateElement("num_burden", burden);
        infoLabel.updateElement("num_objects", objects);
        infoLabel.updateElement("num_chars", stat);
        infoLabel.updateElement("num_ops", ops);
        infoLabel.updateElement("num_tokens", numTokens);
        infoLabel.updateElement("render_time", this.renderTime);
        infoLabel.updateElement("create_time", this.createTime);
    }
    
    
    /**
     * Registers a new rule selection listener.
     */
    public void addParsingResultSelectionListener(ParsingResultSelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    
    /**
     * Draws the connection lines between nodes in the given tree.
     *
     * @param g        Graphics context to draw to.
     * @param parent   Parent tree-node.
     * @param selected Indicates that this is a sub-node of a selected node.
     */
    protected void paintEdges(Graphics g, Tree parent, boolean selected) {
        var sel = selected || isSelectedTreeNode(parent);
        
        var stroke = sel ?
            DefaultStyles.EDGE_STROKE_SELECTED :
            DefaultStyles.EDGE_STROKE_DEFAULT;
        
        ((Graphics2D) g).setStroke(stroke);
        
        var parentBounds =
            getBoundsOfNode(parent);
        
        double x1 = 0;
        double y1 = 0;
        
        var connectorColor = DefaultStyles.getColorFromAppSettings(ColorKey.CONNECTOR_COLOR);
        var selectedColor = DefaultStyles.getColorFromAppSettings(ColorKey.CONNECTOR_SELECTED_COLOR);
        
        switch (layoutOrientation) {
            case Top:
                x1 = parentBounds.getCenterX();
                y1 = parentBounds.getMaxY();
                break;
            case Left:
                x1 = parentBounds.getMaxX();
                y1 = parentBounds.getCenterY();
                break;
            case Right:
                x1 = parentBounds.getX();
                y1 = parentBounds.getCenterY();
                break;
            case Bottom:
                x1 = parentBounds.getCenterX();
                y1 = parentBounds.getY();
        }
        
        for (var i = 0; i < parent.getChildCount(); i++) {
            var child = parent.getChild(i);
            
            var childBounds =
                getBoundsOfNode(child);
            
            double x2 = 0;
            double y2 = 0;
            
            switch (layoutOrientation) {
                case Bottom:
                    x2 = childBounds.getCenterX();
                    y2 = childBounds.getMaxY();
                    break;
                case Right:
                    x2 = childBounds.getMaxX();
                    y2 = childBounds.getCenterY();
                    break;
                case Left:
                    x2 = childBounds.getX();
                    y2 = childBounds.getCenterY();
                    break;
                case Top:
                    x2 = childBounds.getCenterX();
                    y2 = childBounds.getY();
            }
            
            g.setColor(sel ? selectedColor : connectorColor);
            
            if (useCurvedEdges) {
                CubicCurve2D c = new CubicCurve2D.Double();
                var ctrly1 = (y1 + y2) / 2;
                c.setCurve(x1, y1, x1, ctrly1, x2, y1, x2, y2);
                ((Graphics2D) g).draw(c);
            } else {
                var p1 = new Point2D.Double(x1, y1);
                var p2 = new Point2D.Double(x2, y2);
                var ly2 = -(y2 - y1) / 2.0; // vertical half-length
                
                var halfDelta = new Point2D.Double((x2 - x1) / 2., (y2 - y1) / 2.);
                var middle = new Point2D.Double(p1.x + halfDelta.x, p1.y + halfDelta.y);
                var p22 = new Point2D.Double(p1.x, p1.y + halfDelta.y);
                var p11 = new Point2D.Double(p2.x, p2.y - halfDelta.y);
                
                var plr = new PathRenderer(p1, p22, middle, p11, p2);
                plr.render((Graphics2D) g, true);
                
                //g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }
            
            paintEdges(g, child, sel);
        }
    }
    
    
    /**
     * Returns the maximum with in pixels needed by a tree-node, and
     * it's sub-nodes, based on the given root tree-node.
     *
     * @return Text width in pixel.
     */
    public double getMaximumTextWith() {
        if (treeLayout == null || getTree() == null || getTree().getRoot() == null || extentProvider == null)
            return 0;
        
        return getRecursiveMaxTextWidth(getTree().getRoot(), 0);
    }
    
    
    /**
     * Returns the maximum with in pixels needed by a tree-node, and
     * it's sub-nodes, based on the given root tree-node.
     *
     * @param tree    The tree-node to examine.
     * @param current The current width.
     * @return The max width.
     */
    private double getRecursiveMaxTextWidth(Tree tree, double current) {
        var width = current;
        
        var s = getText(tree);
        var bounds = UIHelper.getFullStringBounds(
            (Graphics2D) getGraphics(),
            s,
            DefaultStyles.getDefaultNodeStyle().getFont()
        );
        
        var w = bounds.getWidth() + DefaultStyles.DEFAULT_TEXT_MARGIN.getHorizontal();
        
        width = max(w, width);
        
        for (var i = 0; i < tree.getChildCount(); i++) {
            var n = getRecursiveMaxTextWidth(tree.getChild(i), width);
            width = max(n, width);
        }
        
        return width;
    }
    
    
    /**
     * Returns the exact size of the canvas.
     *
     * @return Dimensions as Rectangle.
     */
    public Rectangle2D getCanvasBounds() {
        return scrollPane.getViewport().getViewRect();
    }
    
    
    /**
     * Returns the gap between nodes (vertical).
     *
     * @return Gap in px.
     */
    public double getGapBetweenNodes() {
        return gapBetweenNodes;
    }
    
    
    /**
     * Test for exceeding the bounds set by min/max gap size.
     *
     * @param gap   Gap size.
     * @param delta Delta in px.
     * @return True if exceeds.
     */
    public boolean exceedsGapBounds(double gap, double delta) {
        return gap + delta > MAX_NODES_GAP ||
            gap + delta < MIN_NODES_GAP;
    }
    
    
    /**
     * Set relative size of nodes gap.
     *
     * @param delta Delta in px.
     */
    public void setRelativeNodesGap(double delta) {
        gapBetweenNodes =
            exceedsGapBounds(gapBetweenNodes, delta) ?
                gapBetweenNodes :
                gapBetweenNodes + delta;
    }
    
    
    /**
     * Set the absolute size of gap between the nodes.
     *
     * @param gapBetweenNodes Absolute gap in px.
     */
    public void setGapBetweenNodes(double gapBetweenNodes) {
        this.gapBetweenNodes = gapBetweenNodes;
    }
    
    
    /**
     * Reset the size of gap between the nodes.
     */
    public void resetGapBetweenNodes() {
        this.gapBetweenNodes = DEFAULT_GAP_BETWEEN_NODES;
    }
    
    
    /**
     * Returns the current scale.
     *
     * @return Scale.
     */
    public double getScale() {
        return scale;
    }
    
    
    /**
     * Set the current scale.
     *
     * @param scale Scale.
     */
    public void setScale(double scale) {
        this.scale = scale;
    }
    
    
    /**
     * Determine the scale-factor used by
     * autoscaling function.
     */
    protected void doAutoScale() {
        if (treeLayout == null)
            return;
        
        Rectangle2D canvasBounds = getCanvasBounds();
        Rectangle2D treeBounds = treeLayout.getBounds();
        
        double xRatio =
            canvasBounds.getWidth() /
                (treeBounds.getWidth() + VIEWER_HORIZONTAL_MARGIN * 2.);
        
        double yRatio =
            canvasBounds.getHeight() /
                (treeBounds.getHeight() + VIEWER_VERTICAL_MARGIN * 2.);
        
        // determine the smallest scale factor
        scale = min(xRatio, yRatio);
        
        // quantise
        //  scale = Math.floor(scale * 100) / 100;
    }
    
    
    /**
     * Increment zoom level by delta factor.
     * Disables auto-scaling.
     *
     * @param delta Delta factor. (0.1 = 10% etc.)
     */
    protected void setRelativeScaling(double delta) {
        autoscaling = false;
        this.scale += delta;
    }
    
    
    /**
     * Set new scale level and reset auto-scaling.
     *
     * @param newScale New scale as factor. (0.1 = 10% etc.)
     */
    protected void setScaleLevel(double newScale) {
        autoscaling = false;
        scale = newScale;
    }
    
    
    /**
     * Computes the correct scale-factor for proper zoom to fit content.
     * Zooming are limited to: 10% - 166%.
     */
    protected void updateScaling() {
        if (autoscaling) {
            doAutoScale();
        }
        
        // use different maximum and minimum scaling
        // if auto-scale is turned on
        double maxScale = autoscaling ?
            MAX_AUTO_SCALE_FACTOR :
            MAX_SCALE_FACTOR;
        
        double minScale = autoscaling ?
            MIN_AUTO_SCALE_FACTOR :
            MIN_SCALE_FACTOR;
        
        // check scaling boundaries
        scale = min(scale, maxScale);
        scale = max(scale, minScale);
    }
    
    
    /**
     * Compute the offset...
     */
    private void updateOffset() {
        Rectangle viewport =
            scrollPane.getViewportBorderBounds();
        
        double offsetX = 0;
        
        if (getScaledTreeSize().getWidth() < viewport.getWidth()) {
            offsetX = viewport.getWidth() / 2. - getScaledTreeSize().getWidth() / 2.;
            offsetX *= (1. / scale);
        }
        
        double offsetY = 0;
        
        if (getScaledTreeSize().getHeight() < viewport.getHeight()) {
            offsetY = viewport.getHeight() / 2. - getScaledTreeSize().getHeight() / 2.;
            offsetY *= (1. / scale);
        }
        
        offset.setLocation(
            offsetX,
            offsetY
        );
        
        //infoLabel.setOffset(new Point(getWidth() - infoLabel.getWidth(), getHeight() - infoLabel.getHeight()));
    }
    
    
    /**
     * Paint component.
     *
     * @param g Graphics context.
     * @see javax.swing.JComponent
     */
    @Override
    public void paint(Graphics g) {
        this.setBackground(DefaultStyles.getColorFromAppSettings(ColorKey.VIEWER_BACKGROUND));
        super.paint(g);
        
        // no parse-tree generated
        if (!hasTreeLayout() && root == null)
            return;
        
        // force tree-layout, if no layout done yet
        if (!hasTreeLayout())
            treeInvalidated = true;
        
        // trigger relayout if margin-box has been set for scrolling
        if (marginBox != null)
            treeInvalidated = true;
        
        // capture timestamp
        var time = System.nanoTime();
        
        // detect any canvas size change
        var sizeChanged =
            viewport == null ||
                !getSize().equals(viewport) ||
                !getSize().equals(getParent().getSize());
        
        sizeChanged = !treeInvalidated && sizeChanged;
        
        // do a complete relayout if update flag has been set
        if (treeInvalidated) {
            doTreeLayout();         // create a new tree-layout based on the parse-tree
            
            // tree-layout could not be set
            if (!hasTreeLayout())
                return;
            
            updateScaling();        // compute the proper scaling factor
            updateOffset();         // compute the offset and margin of layout
            updateStyledTreeNodes();// transform the parse-tree to styled-nodes
            updatePreferredSize();  // update the canvas size
        }
        
        // just update scaling and offset on canvas size changes
        if (sizeChanged) {
            updateScaling();        // compute the proper scaling factor
            updateOffset();         // compute the offset and margin of layout
            updatePreferredSize();  // update the canvas size
        }
        
        var g2 = (Graphics2D) g;
        
        // anti-alias the lines
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        
        // set fractional metrics ON to improve text rendering quality
        g2.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        
        // anti-alias text, default aa
        g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
        
        // set global canvas scale
        g2.scale(scale, scale);
        
        // update view
        if (marginBox != null) {
            scrollRectToVisible(marginBox);
            marginBox = null;
        }
        
        // repaint nodes and connectors
        if (root != null && styledRootNode != null) {
            paintEdges(g, getTree().getRoot(), false);
            updateStyledTreeNodes();
            styledRootNode.render(g2);
        }
        
        renderTime = (renderTime + ((double) (System.nanoTime() - time) / 1_000_000.)) / 2.0;
        
        // Update stats and render-time on tree changed
        if (treeInvalidated) {
            updateParseData(); // update parsing stats for info text
            
            treeInvalidated = false;// reset flag
        }
    }
    
    
    /**
     * Returns the time needed to render all nodes in the current context.
     *
     * @return Time in milliseconds.
     */
    public double getRenderTime() {
        return renderTime;
    }
    
    
    /**
     * Text if compact labeling mode is used for node labeling.
     *
     * @return True is compact is used.
     */
    public boolean isCompactLabels() {
        return compactLabels;
    }
    
    
    /**
     * Set compact labeling for tree-nodes.
     *
     * @param compactLabels Flag: true=On, false=Off
     */
    public void setCompactLabels(boolean compactLabels) {
        this.compactLabels = compactLabels;
        
        if (treeTextProvider != null && treeTextProvider instanceof AltLabelTextProvider) {
            AltLabelTextProvider altLabelTextProvider = (AltLabelTextProvider) treeTextProvider;
            altLabelTextProvider.setCompact(compactLabels);
        }
        
        setTreeInvalidated(true);
    }
    
    
    /**
     * Returns the bounds of a tree node including the offset vector.
     *
     * @param node The tree node.
     * @return Bounds as {@code Rectangle2D}.
     */
    protected Rectangle2D.Double getBoundsOfNode(Tree node) {
        Rectangle2D.Double bounds = treeLayout.getNodeBounds().get(node);
        return new Rectangle2D.Double(
            bounds.x + offset.getX(),
            bounds.y + offset.getY(),
            bounds.width, bounds.height
        );
    }
    
    
    /**
     * Get tree node label from: {@code TreeTextProvider}.
     *
     * @param tree The tree node.
     * @return The labels as string.
     * @see AltLabelTextProvider
     */
    protected String getText(Tree tree) {
        var s = treeTextProvider.getText(tree);
        //  s = Utils.escapeWhitespace(s.trim(), false);
        
        if (isRootNode(tree))
            s += "\nstart-rule";
        
        return s.trim();
    }
    
    
    /**
     * Returns the scaled size of the tree-layout.
     *
     * @return Size as Dimension2D.
     */
    public Dimension2D getScaledTreeSize() {
        return new Dimension2DDouble(
            treeLayout.getBounds().getWidth() * scale,
            treeLayout.getBounds().getHeight() * scale
        );
        
    }
    
    
    /**
     * Returns the original size of the tree-layout.
     *
     * @return Size as Dimension2D.
     */
    public Dimension2D getTreeSize() {
        return new Dimension2DDouble(
            treeLayout.getBounds().getWidth(),
            treeLayout.getBounds().getHeight()
        );
        
    }
    
    
    /**
     * Checks if the scaled tree dimension exceeds the visible viewport.
     *
     * @return True if tree-view > scrollbar dimension.
     */
    protected boolean treeBoundsExceedViewport() {
        Rectangle viewport = scrollPane.getViewportBorderBounds();
        return getScaledTreeSize().getWidth() > viewport.getWidth() ||
            getScaledTreeSize().getHeight() > viewport.getHeight();
    }
    
    
    /**
     * Checks if the scaled tree dimension exceeds the components size.
     * l
     *
     * @return True if tree-view > getSize().
     */
    protected boolean treeBoundsExceedCanvas() {
        Dimension bounds = getSize();
        
        return getScaledTreeSize().getWidth() > bounds.getWidth() ||
            getScaledTreeSize().getHeight() > bounds.getHeight();
    }
    
    
    /**
     * Update the component's size based on the tree layout size.
     */
    protected void updatePreferredSize() {
        this.setPreferredSize(
            new Dimension(
                (int) getScaledTreeSize().getWidth(),
                (int) getScaledTreeSize().getHeight()
            )
        );
        
        if (getParent() != null)
            getParent().revalidate();
        
        viewport = getSize();
    }


//    /**
//     * Helper method to draw text on a Graphics context.
//     * Whitespaces are escaped except spaces.
//     *
//     * @param g Graphics context
//     * @param s The string to be drawn.
//     * @param x The X coordinate to draw at.
//     * @param y The Y coordinate to draw at.
//     */
//    public void text(Graphics g, String s, int x, int y) {
//         s = Utils.escapeWhitespace(s, false);
//        g.drawString(s, x, y);
//    }
    
    
    /**
     * Get the TreeTextProvider {@link TreeTextProvider}
     *
     * @return TreeTextProvider
     */
    public TreeTextProvider getTreeTextProvider() {
        return treeTextProvider;
    }
    
    
    /**
     * Sets the TreeTextProvider. {@link TreeTextProvider}
     *
     * @param treeTextProvider TreeTextProvider
     */
    public void setTreeTextProvider(TreeTextProvider treeTextProvider) {
        this.treeTextProvider = treeTextProvider;
        
        if (treeTextProvider instanceof AltLabelTextProvider) {
            AltLabelTextProvider altLabelTextProvider = (AltLabelTextProvider) treeTextProvider;
            altLabelTextProvider.setCompact(compactLabels);
        }
    }
    
    
    /**
     * Get an adaptor for root that indicates how to walk ANTLR trees.
     * Override to change the adaptor from the default of {@link TreeLayoutAdaptor}
     */
    public TreeForTreeLayout<Tree> getTreeLayoutAdaptor(Tree root) {
        return new TreeLayoutAdaptor(root);
    }
    
    
    /**
     * Compute tree-layout based on root node.
     */
    public void doTreeLayout() {
        var time = System.nanoTime();
        // reset if no tree instance has been passed
        if (root == null) {
            treeLayout = null;
            styledRootNode = null;
            this.infoLabel.setVisible(false);
            repaint();
            
            return;
        }
        
        var nodesGap =
            isCompactLabels() ?
                gapBetweenNodes * COMPACT_LABELS_FACTOR_HORIZONTAL :
                gapBetweenNodes;
        
        var configuration =
            new DefaultConfiguration<Tree>(
                nodesGap,
                nodesGap,
                layoutOrientation,
                AlignmentInLevel.AwayFromRoot
            );
        
        treeLayout = new TreeLayout<>(
            getTreeLayoutAdaptor(root),
            extentProvider,
            configuration,
            true
        );
        
        this.infoLabel.setVisible(true);
        
        createTime = (double) (System.nanoTime() - time) / 1_000_000;
    }
    
    
    /**
     * Creates a tree layout by out of a tree node.
     *
     * @param root The root node of the tree.
     */
    public void setTree(Tree root) {
        if (root == null) {
            this.root = null;
            return;
        }
        
        parseTime = previewState.parseTime;
        this.root = root;
        setTreeInvalidated(true);
    }
    
    
    /**
     * Test for updated tree flag.
     *
     * @return True if tree has been touched.
     */
    public boolean isTreeInvalidated() {
        return treeInvalidated;
    }
    
    
    /**
     * Set tree updated flag.
     *
     * @param treeInvalidated Flag to set.
     */
    public void setTreeInvalidated(boolean treeInvalidated) {
        this.treeInvalidated = treeInvalidated;
        repaint();
    }
    
    
    /**
     * Set rule names.
     *
     * @param ruleNames List of rule-names as string.
     */
    public void setRuleNames(List<String> ruleNames) {
        setTreeTextProvider(
            new DefaultTreeTextProvider(ruleNames)
        );
    }
    
    
    /**
     * return the Tree from the Tree-layouter.
     *
     * @return Layouted tree.
     */
    protected TreeForTreeLayout<Tree> getTree() {
        return treeLayout.getTree();
    }
    
    
    /**
     * Create StyledTreeNodes from the parse-tree-nodes.
     */
    protected void updateStyledTreeNodes() {
        styledRootNode = new BasicStyledElement();
        
        var viewport =
            new Rectangle2D.Double(
                offset.getX(),
                offset.getY(),
                getScaledTreeSize().getWidth(),
                getScaledTreeSize().getHeight()
            );
        
        // root node
        styledRootNode.setViewport(viewport);
        
        objects = 0;
        // paint the boxes
        for (var tree : treeLayout.getNodeBounds().keySet()) {
            styledRootNode.add(
                treeNodeToStyledElement(tree)
            );
            objects++;
        }
    }
    
    
    /**
     * Creates a StyledElement out of a tree-node.
     *
     * @param tree Tree node to convert.
     * @return The corresponding StyledElement.
     */
    public StyledElement treeNodeToStyledElement(Tree tree) {
        var bounds = getBoundsOfNode(tree);
        
        StyledTreeNode node =
            new BasicStyledTreeNode(
                styledRootNode,
                bounds,
                DefaultStyles.getDefaultNodeStyle(),
                isSelectedTreeNode(tree),
                isCompactLabels()
            );
        
        var ruleFailed = false;
        if (tree instanceof ParserRuleContext) {
            var ctx = (ParserRuleContext) tree;
            ruleFailed =
                ctx.exception != null &&
                    ctx.stop != null &&
                    ctx.stop.getTokenIndex() < ctx.start.getTokenIndex();
        }
        
        /* --------------------------------------------------------------------- */
        
        if (tree instanceof ErrorNode || ruleFailed)
            node = new ErrorTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree),
                isCompactLabels()
            );
        
        if (tree instanceof TerminalNode)
            node = new TerminalTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree),
                isCompactLabels()
            );
        
        // treat only as 'real' EOF node, if not in re-sync mode!
        if (isEOFNode(tree) && !isReSyncedNode(tree)) {
            node = new EOFTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree),
                isCompactLabels()
            );
        }
        
        if (isRootNode(tree))
            node = new RootTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree),
                isCompactLabels()
            );
        
        if (isReSyncedNode(tree)) {
            if (isSelectedTreeNode(tree)) {
                node.setOutlineColor((JBColor) DefaultStyles.getDefaultResyncStyle().getBackground().brighter());
                node.getShape().getStyleProperties().setStroke(DefaultStyles.getDefaultResyncStyle().getStroke());
            } else {
                node.setOutlineColor(DefaultStyles.getDefaultResyncStyle().getBackground());
            }
            
        }
        
        // add node text
        node.setText(getText(tree));
        
        return node;
    }
    
    
    /**
     * Test if a valid tree layout has been assigned.
     *
     * @return True if assigned.
     */
    public boolean hasTreeLayout() {
        return treeLayout != null/* && root != null && styledRootNode != null*/;
    }
    
    
    /**
     * Test tree-node for root-node.
     *
     * @param tree Tree.
     * @return True if is root node-
     */
    public boolean isRootNode(Tree tree) {
        return tree.getParent() == null;
    }
    
    
    public boolean isTerminalNode(Tree tree) {
        return tree instanceof TerminalNode && tree.getChildCount() == 0;
    }
    
    
    /**
     * Test tree-node for eof-node.
     *
     * @param tree Tree.
     * @return True if EOF.
     */
    public boolean isEOFNode(Tree tree) {
        return (
            tree instanceof TerminalNode &&
                Objects.equals(((TerminalNode) tree).getSymbol().getText(), "<EOF>")
        );
    }
    
    
    /**
     * Test if the given node is part of re-sync.
     *
     * @param tree Tree.
     * @return True is part of re-sync.
     */
    public boolean isReSyncedNode(Tree tree) {
        return tree instanceof ErrorNodeImpl;
    }
    
    
    /**
     * Test if the given Point2D hits any node that can be selected.
     *
     * @param p XY coordinate
     */
    protected void testForNodeSelection(Point2D p) {
        // do nothing on an invalid tree
        if (treeLayout == null || treeLayout.getLevelCount() == 0) return;
        
        Tree node = getNodeFromLocation(p);
        
        if (node != null) {
            // set selected node
            setSelectedTreeNode(node);
            
            // update object explorer
            previewPanel.getPropertiesPanel().setTreeNode(
                (ParseTree) node,
                (AltLabelTextProvider) getTreeTextProvider()
            );
            
            Rectangle2D nodeBounds = getBoundsOfNode(node);
            
            // prevent that selected nodes are partly covered by the border
            marginBox = new Rectangle(
                (int) Math.round(nodeBounds.getX() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getY() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getWidth() * scale + SCROLL_VIEWPORT_MARGIN * 2),
                (int) Math.round(nodeBounds.getHeight() * scale + SCROLL_VIEWPORT_MARGIN * 2)
            );
            // if needed, scroll node to be fully visible
            scrollRectToVisible(marginBox);
        } else {
            // nothing selected
            previewPanel.getPropertiesPanel().clear();
            setSelectedTreeNode(null);
        }
        
        
        // raise event for all selection listeners
        for (ParsingResultSelectionListener listener : selectionListeners) {
            listener.onParserRuleSelected(node);
        }
        
        previewPanel.repaint();
        repaint();
    }
    
    
    /**
     * Set proper viewport and scaling to fit to a specific node.
     */
    protected void focusSelectedNode() {
        if (!hasTreeLayout() || getSelectedTreeNode() == null)
            return;
        
        setScaleLevel(NODE_FOCUS_SCALE_FACTOR);
        
        Tree node = getSelectedTreeNode();
        
        Rectangle2D bounds = scrollPane.getViewportBorderBounds();
        Rectangle2D nodeBounds = getBoundsOfNode(node);
        
        double centerX = nodeBounds.getCenterX();
        double centerY = nodeBounds.getCenterY();
        
        // 90% of the current viewport size
        double alpha = 0.9;
        double marginH = bounds.getWidth() / 2. * alpha;
        double marginV = bounds.getHeight() / 2. * alpha;
        
        marginBox = new Rectangle(
            (int) Math.round(centerX * scale - marginH),
            (int) Math.round(centerY * scale - marginV),
            (int) Math.round(2 * marginH),
            (int) Math.round(2 * marginV)
        );
        
        repaint();
    }
    
    
    /**
     * Try to find a node at the given location.
     *
     * @param p XY coordinate
     * @return An instance of Tree if the location matches a node, NULL otherwise.
     */
    private Tree getNodeFromLocation(Point2D p) {
        // do nothing on an invalid tree
        if (treeLayout == null || treeLayout.getLevelCount() == 0) return null;
        
        for (Tree tree : treeLayout.getNodeBounds().keySet()) {
            Rectangle2D.Double box = getBoundsOfNode(tree);
            if (box.contains(p.getX() / scale, p.getY() / scale)) {
                return tree;
            }
        }
        
        return null;
    }
    
    
    /**
     * Checks if a given location hits a node.
     *
     * @param p Location as Pout2D.
     * @return True if hits a node.
     */
    private boolean locationHitsNode(Point2D p) {
        return getNodeFromLocation(p) != null;
    }
    
    
    /**
     * Checks for selected tree node.
     *
     * @param tree The tree node.
     * @return True if node matches.
     */
    public boolean isSelectedTreeNode(Tree tree) {
        return selectedTreeNode != null &&
            tree == selectedTreeNode;
    }
    
    
    /**
     * Get selected tree node.
     *
     * @return Tree node instance. {@code Tree}
     */
    public Tree getSelectedTreeNode() {
        return selectedTreeNode;
    }
    
    
    /**
     * Set the current selected tree node.
     *
     * @param tree The tree node.
     */
    public void setSelectedTreeNode(Tree tree) {
        this.selectedTreeNode = tree;
    }
    
    
    /**
     * Returns the assigned tree layout.
     *
     * @return The tree layout: {@code TreeLayout}.
     */
    public TreeLayout<Tree> getTreeLayout() {
        return treeLayout;
    }
    
    /* --------------------------------------------------------------------- */
    
    
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {}
    
    
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
            lastMousePos = mouseEvent.getPoint();
            testForNodeSelection(mouseEvent.getPoint());
        }
    }
    
    
    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (treeLayout == null) return;
        
        deltaMousePos.setLocation(0, 0);
        
        // reset cursor
        if (!locationHitsNode(mouseEvent.getPoint())) {
            setCursor(DEFAULT_CURSOR);
        }
        
        repaint();
    }
    
    
    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}
    
    
    @Override
    public void mouseExited(MouseEvent mouseEvent) {}
    
    
    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (treeLayout == null)
            return;
        
        setCursor(DRAG_CURSOR);
        deltaMousePos.setLocation(
            lastMousePos.getX() - mouseEvent.getX(),
            lastMousePos.getY() - mouseEvent.getY()
        );
        
        if (treeBoundsExceedViewport()) {
            int hval = scrollPane.getHorizontalScrollBar().getValue();
            int vval = scrollPane.getVerticalScrollBar().getValue();
            
            scrollPane.getHorizontalScrollBar().setValue(hval + (int) (deltaMousePos.getX()));
            scrollPane.getVerticalScrollBar().setValue(vval + (int) (deltaMousePos.getY()));
            
            repaint();
        }
    }
    
    
    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (treeLayout == null)
            return;
        
        currentMousePos = mouseEvent.getPoint();
        // check if there is a node under the mouse cursor
        if (locationHitsNode(currentMousePos)) {
            Tree tree = getNodeFromLocation(currentMousePos);
            setCursor(SELECT_CURSOR);
            setToolTipText(getTreeTextProvider().getText(tree));
        } else {
            setCursor(DEFAULT_CURSOR);
            setToolTipText("");
            
        }
        repaint();
    }
}
