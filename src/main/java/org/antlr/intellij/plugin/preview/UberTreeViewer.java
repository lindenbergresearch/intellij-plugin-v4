package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import org.abego.treelayout.Configuration.AlignmentInLevel;
import org.abego.treelayout.Configuration.Location;
import org.abego.treelayout.TreeForTreeLayout;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.intellij.plugin.preview.ui.BasicStyledElement;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.StyledElement;
import org.antlr.intellij.plugin.preview.ui.UIHelper;
import org.antlr.intellij.plugin.preview.ui.treenodes.*;
import org.antlr.v4.gui.TreeLayoutAdaptor;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.ParserRuleContext;
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
    public final static double MAX_SCALE_FACTOR = 2.0;
    public final static double MIN_SCALE_FACTOR = 0.1;
    public final static double SCALING_INCREMENT = 0.15;
    public final static double NODE_FOCUS_MARGIN = 140;
    public final static double NODE_FOCUS_SCALE_FACTOR = 1.25;
    public final static int VIEWER_HORIZONTAL_MARGIN = 26;
    public final static int SCROLL_VIEWPORT_MARGIN = 30;
    public static final double COMPACT_LABELS_FACTOR = 0.4;
    
    /*---- CURSOR -------------------------------------------------------------------------------*/
    public static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    public static final Cursor SELECT_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    public static final Cursor DRAG_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    
    /*---- COLORS -------------------------------------------------------------------------------*/
    protected JBColor edgesColor;
    
    /*---- MOUSE --------------------------------------------------------------------------------*/
    private Point2D lastMousePos, currentMousePos, deltaMousePos;
    
    private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();
    protected JScrollPane scrollPane;
    protected VariableExtentProvider extentProvider;
    
    protected int minCellWidth, count;
    protected float edgesStrokeWidth;
    protected boolean autoscaling;
    protected double scale, oldScale;
    protected double renderTime;
    protected boolean compactLabels;
    protected boolean treeInvalidated;
    protected Point2D offset;
    protected Dimension viewport;
    protected Tree selectedTreeNode;
    protected BasicStyledElement styledRootNode;
    
    protected Font font;
    boolean useCurvedEdges;
    
    protected double gapBetweenLevels;
    protected double gapBetweenNodes;
    
    
    protected TreeLayout<Tree> treeLayout;
    protected Tree root;
    protected TreeTextProvider treeTextProvider;
    
    public PreviewPanel previewPanel;
    
    
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
     * Creates the UberTreeViewer component based on the given tree node.
     *
     * @param previewPanel Reference to the PreviewPanel.
     */
    public UberTreeViewer(PreviewPanel previewPanel) {
        this.previewPanel = previewPanel;
        this.setBackground(DefaultStyles.getConsoleBackground());
        
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
        gapBetweenLevels = 30;
        gapBetweenNodes = 30;
        
        scale = 1.f;
        autoscaling = true;
        
        compactLabels = false;
        
        // add handler for mouse events
        addMouseListener(this);
        addMouseMotionListener(this);
        
        lastMousePos = new Point(0, 0);
        currentMousePos = new Point(0, 0);
        deltaMousePos = new Point(0, 0);
        
        setAutoscrolls(true);
        
        
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
        boolean sel = selected || isSelectedTreeNode(parent);
        
        Stroke stroke = sel ?
            DefaultStyles.EDGE_STROKE_SELECTED :
            DefaultStyles.EDGE_STROKE_DEFAULT;
        
        ((Graphics2D) g).setStroke(stroke);
        
        Rectangle2D.Double parentBounds = getBoundsOfNode(parent);
        double x1 = parentBounds.getCenterX();
        double y1 = parentBounds.getMaxY();
        
        for (int i = 0; i < parent.getChildCount(); i++) {
            Tree child = parent.getChild(i);
            
            Rectangle2D.Double childBounds = getBoundsOfNode(child);
            double x2 = childBounds.getCenterX();
            double y2 = childBounds.getMinY();
            
            if (sel) g.setColor(DefaultStyles.EDGE_COLOR_SELECTED);
            else g.setColor(DefaultStyles.EDGE_COLOR_DEFAULT);
            
            if (useCurvedEdges) {
                CubicCurve2D c = new CubicCurve2D.Double();
                double ctrly1 = (y1 + y2) / 2;
                c.setCurve(x1, y1, x1, ctrly1, x2, y1, x2, y2);
                ((Graphics2D) g).draw(c);
            } else {
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
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
        if (
            treeLayout == null ||
            getTree() == null ||
            getTree().getRoot() == null ||
            extentProvider == null
        )
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
        double width = current;
        
        String s = getText(tree);
        Dimension bounds = UIHelper.getFullStringBounds(
            (Graphics2D) getGraphics(),
            s,
            DefaultStyles.REGULAR_FONT
        );
        
        double w =
            bounds.getWidth() +
            DefaultStyles.DEFAULT_TEXT_MARGIN.getHorizonal();
        
        width = max(w, width);
        
        for (int i = 0; i < tree.getChildCount(); i++) {
            double n = getRecursiveMaxTextWidth(tree.getChild(i), width);
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
        Rectangle2D canvasBounds = getCanvasBounds();
        Rectangle2D treeBounds = treeLayout.getBounds();
        
        double xRatio =
            (canvasBounds.getWidth()) /
            (treeBounds.getWidth() + offset.getX());
        
        
        double yRatio =
            (canvasBounds.getHeight()) /
            (treeBounds.getHeight() + offset.getY());
        
        // determine the smallest scale factor
        scale = min(xRatio, yRatio);
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
        
        // clamp scale factor
        scale = min(scale, MAX_SCALE_FACTOR);
        scale = max(scale, MIN_SCALE_FACTOR);
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
        
        offset.setLocation(offsetX, offsetY);
    }
    
    
    /**
     * Paint component.
     *
     * @param g Graphics context.
     * @see javax.swing.JComponent
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        ArrayList<String> buff = new ArrayList<>();
        buff.add("PARAMETER:\n");
        buff.add("**********\n\n");
        buff.add("parent        : " + getParent().getWidth() + 'x' + getParent().getHeight());
        
        // force tree-layout, if no layout done yet
        if (!hasTreeLayout())
            treeInvalidated = true;
        
        // capture timestamp
        long time = System.nanoTime();
        
        // detect any canvas size change
        boolean sizeChanged =
            viewport == null ||
            !getSize().equals(viewport) ||
            !getSize().equals(getParent().getSize());
        
        sizeChanged = !treeInvalidated && sizeChanged;
        
        // do a complete relayout if update flag has been set
        if (treeInvalidated) {
            doTreeLayout();         // create a new tree-layout based on the parse-tree
            updateScaling();        // compute the proper scaling factor
            updateOffset();         // compute the offset and margin of layout
            updateStyledTreeNodes();// transform the parse-tree to styled-nodes
            updatePreferredSize();  // update the canvas size
            treeInvalidated = false;// reset flag
        }
        
        
        // just update scaling and offset on canvas size changes
        if (sizeChanged) {
            updateScaling();        // compute the proper scaling factor
            updateOffset();         // compute the offset and margin of layout
            updatePreferredSize();  // update the canvas size
        }
        
        
        buff.add("scale, offset : " + String.format("%.3f", scale) + " | x=" + String.format("%.3f", offset.getX()) + " y=" + String.format(
            "%.3f",
            offset.getY()
        ));
        buff.add("component     : " + getWidth() + 'x' + getHeight());
        buff.add("max text-width: " + getRecursiveMaxTextWidth(getTree().getRoot(), 0) + "px");
        buff.add("tree size     : " + treeLayout.getBounds().getWidth() * scale + 'x' + treeLayout.getBounds().getHeight() * scale);
        buff.add("tree scaled   : " + String.format("%.3f", treeLayout.getBounds().getWidth() * scale) + 'x' + String.format("%.3f", treeLayout.getBounds().getHeight() * scale));
        
        
        Point2D offsetText = new Point(0, 0);
        if (scrollPane != null) {
            offsetText.setLocation(scrollPane.getHorizontalScrollBar().getValue() / scale, scrollPane.getVerticalScrollBar().getValue() / scale);
            buff.add("scrollbars    : " + scrollPane.getHorizontalScrollBar().getValue() + " : " + scrollPane.getVerticalScrollBar().getValue());
            buff.add("scrollbars    : " + offsetText.getX() + " : " + offsetText.getY());
            buff.add("scroll val    : " + String.format("%.3f", scrollPane.getHorizontalScrollBar().getMaximum() / scale) + " - " + String.format("%.3f", scrollPane.getVerticalScrollBar().getMaximum() / scale));
            buff.add("mouse pos     : " + currentMousePos.toString().substring(14));
            buff.add("mouse last    : " + lastMousePos.toString().substring(14));
            buff.add("mouse delta   : " + deltaMousePos.toString().substring(14));
            buff.add("viewport      : " + scrollPane.getViewport().getViewRect().toString().substring(18).replace("width", "w").replace("height", "h"));
            buff.add("viewport-bb   : " + scrollPane.getViewportBorderBounds().toString().substring(18).replace("width", "w").replace("height", "h"));
            //  buff.add("viewport      : " + scrollPane.getViewport().toString().substring(18).replace("width", "w").replace("height", "h"));
            
        }
        
        buff.add("\n");
        
        Graphics2D g2 = (Graphics2D) g;
        
        // anti-alias the lines
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        //  g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        
        // this has to be turned on to proper render all text positions
        // if set to 'on' labels will not be proper layouted.
        g2.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_OFF);
        
        // Anti-alias the text
        g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
        
        g2.scale(scale, scale);
        
        if (root != null && styledRootNode != null) {
            paintEdges(g, getTree().getRoot(), false);
            updateStyledTreeNodes();
            styledRootNode.render(g2);
        }
        
        g2.setColor(JBColor.ORANGE);
        g2.drawRoundRect(
            (int) offset.getX(),
            (int) offset.getY(),
            (int) getTreeSize().getWidth(),
            (int) getTreeSize().getHeight(),
            5, 5
        );

//        g2.setColor(JBColor.PINK);
//        g2.drawRoundRect(
//            (int) scrollPane.,
//            (int) offset.getY(),
//            getScaledTreeSize().width,
//            getScaledTreeSize().height,
//            5, 5
//        );
//
        
        double delta = ((double) System.nanoTime() - time) / 1_000_000.;
        
        Font saved = g2.getFont();
        Font menlo = new Font("Menlo", Font.PLAIN, 10);
        
        g2.setFont(menlo.deriveFont((float) (13. * (1. / scale))).deriveFont(Font.BOLD));
        g2.setColor(JBColor.background().brighter());
        
        buff.add("render no     : #" + count++ + " redraws");
        buff.add("r-time        : " + String.format("%.3f", delta) + "ms");
        
        double y = 10. + offsetText.getY();
        for (String s : buff) {
            g2.drawString(s, (float) (10. + offsetText.getX()), (float) y);
            y += 18 * (1. / scale);
        }
        
        g2.setFont(saved);
        
        LOG.info("paint() time:" + String.format("%.3f", delta) + "ms");
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
        String s = treeTextProvider.getText(tree);
        //  s = Utils.escapeWhitespace(s.trim(), false);
        return s.trim();
    }


//    /**
//     * Returns the dimension of the tree layout
//     *
//     * @return Dimension of the rendered tree.
//     */
//    private Dimension getScaledTreeSize() {
//        Dimension scaledTreeSize =
//            treeLayout.getBounds().getBounds().getSize();
//
//        return new Dimension(
//            (int) Math.round(scaledTreeSize.width * scale),
//            (int) Math.round(scaledTreeSize.height * scale + VIEWER_HORIZONTAL_MARGIN)
//        );
//    }
//
    
    
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
    
    
    /**
     * Helper method to draw text on a Graphics context.
     * Whitespaces are escaped except spaces.
     *
     * @param g Graphics context
     * @param s The string to be drawn.
     * @param x The X coordinate to draw at.
     * @param y The Y coordinate to draw at.
     */
    public void text(Graphics g, String s, int x, int y) {
        // s = Utils.escapeWhitespace(s, false);
        g.drawString(s, x, y);
    }
    
    
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
        // reset if no tree instance has been passed
        if (root == null) {
            treeLayout = null;
            styledRootNode = null;
            repaint();
            return;
        }
        
        double verticalGap =
            isCompactLabels() ?
                gapBetweenLevels * COMPACT_LABELS_FACTOR :
                gapBetweenLevels;
        
        double horizontalGap =
            isCompactLabels() ?
                gapBetweenNodes * COMPACT_LABELS_FACTOR :
                gapBetweenNodes;
        
        DefaultConfiguration<Tree> configuration =
            new DefaultConfiguration<>(
                verticalGap,
                horizontalGap,
                Location.Top,
                AlignmentInLevel.AwayFromRoot
            );
        
        treeLayout = new TreeLayout<>(
            getTreeLayoutAdaptor(root),
            extentProvider,
            configuration,
            true
        );
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
        
        Rectangle2D.Double viewport =
            new Rectangle2D.Double(
                offset.getX(),
                offset.getY(),
                getScaledTreeSize().getWidth(),
                getScaledTreeSize().getHeight()
            );
        
        // root node
        styledRootNode.setViewport(viewport);
        
        int k = 0;
        
        // paint the boxes
        for (Tree tree : treeLayout.getNodeBounds().keySet()) {
            styledRootNode.add(
                treeNodeToStyledElement(tree)
            );
            k++;
        }
        
        if (selectedTreeNode != null)
            previewPanel.getPropertiesPanel().setTreeNode((ParseTree) selectedTreeNode);
    }
    
    
    /**
     * Creates a StyledElement out of a tree-node.
     *
     * @param tree Tree node to convert.
     * @return The corresponding StyledElement.
     */
    public StyledElement treeNodeToStyledElement(Tree tree) {
        Rectangle2D.Double bounds = getBoundsOfNode(tree);
        
        StyledTreeNode node =
            new BasicStyledTreeNode(
                styledRootNode,
                bounds,
                DefaultStyles.DEFAULT_STYLE,
                isSelectedTreeNode(tree)
            );
        
        boolean ruleFailed = false;
        if (tree instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) tree;
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
                isSelectedTreeNode(tree)
            );
        
        if (tree instanceof TerminalNode)
            node = new TerminalTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree)
            );
        
        if (isEOFNode(tree)) {
            node = new EOFTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree)
            );
        }
        
        if (isRootNode(tree))
            node = new RootTreeNode(
                styledRootNode,
                bounds,
                isSelectedTreeNode(tree)
            );
        
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
    
    
    /**
     * Test tree-node for eof-node.
     *
     * @param tree Tree.
     * @return True if EOF.
     */
    public boolean isEOFNode(Tree tree) {
        return (
            tree instanceof TerminalNode &&
            ((TerminalNode) tree).getText().equals(AltLabelTextProvider.EOF_LABEL)
        );
    }
    
    
    /**
     * Test if the given Point2D hits any node that can be selected.
     *
     * @param p XY coordinate
     */
    protected void testForNodeSelection(Point2D p) {
        // do nothing on an invalid tree
        if (treeLayout == null || treeLayout.getLevelCount() == 0) return;
        
        // always clear on click
        setSelectedTreeNode(null);
        
        Tree node = getNodeFromLocation(p);
        
        if (node != null) {
            // set selected node
            setSelectedTreeNode(node);
            
            Rectangle2D nodeBounds = getBoundsOfNode(node);
            Rectangle marginBox = new Rectangle(
                (int) Math.round(nodeBounds.getX() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getY() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getWidth() * scale + SCROLL_VIEWPORT_MARGIN * 2),
                (int) Math.round(nodeBounds.getHeight() * scale + SCROLL_VIEWPORT_MARGIN * 2)
            );
            
            scrollRectToVisible(marginBox);
        }
        
        // raise event for all selection listeners
        for (ParsingResultSelectionListener listener : selectionListeners) {
            listener.onParserRuleSelected(node);
        }
        
        repaint();
    }
    
    
    /**
     * Set proper viewport and scaling to fit to a specific node.
     */
    protected void focusSelectedNode() {
        if (!hasTreeLayout()) return;
        
        Tree node = getSelectedTreeNode();
        
        if (node != null) {
            setScaleLevel(NODE_FOCUS_SCALE_FACTOR);
            updateScaling();
            updatePreferredSize();
            
            Rectangle2D nodeBounds = getBoundsOfNode(node);
            Rectangle marginBox = new Rectangle(
                (int) Math.round(nodeBounds.getX() * scale - NODE_FOCUS_MARGIN),
                (int) Math.round(nodeBounds.getY() * scale - NODE_FOCUS_MARGIN),
                (int) Math.round(nodeBounds.getWidth() * scale + NODE_FOCUS_MARGIN * 2),
                (int) Math.round(nodeBounds.getHeight() * scale + NODE_FOCUS_MARGIN * 2)
            );
            
            scrollRectToVisible(marginBox);
            
            repaint();
        }
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
        }
        repaint();
    }
}
