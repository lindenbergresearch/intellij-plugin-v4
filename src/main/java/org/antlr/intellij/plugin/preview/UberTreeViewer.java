package org.antlr.intellij.plugin.preview;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import org.abego.treelayout.NodeExtentProvider;
import org.abego.treelayout.TreeLayout;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static java.awt.RenderingHints.*;


/**
 * Custom tree layout viewer component.
 * Enhanced version based on: {@code TreeViewer}
 */
public class UberTreeViewer extends TreeViewer implements MouseListener, MouseMotionListener {
    /*---- CONSTANTS ----------------------------------------------------------------------------*/
    public final static double MAX_SCALE_FACTOR = 1.8;
    public final static double MIN_SCALE_FACTOR = 0.05;
    public final static double SCALING_INCREMENT = 0.15;
    public static final int VIEWER_HORIZONTAL_MARGIN = 26;
    public static final int SCROLL_VIEWPORT_MARGIN = 30;

    /*---- CURSOR -------------------------------------------------------------------------------*/
    public static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    public static final Cursor SELECT_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    public static final Cursor DRAG_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    /*---- COLORS -------------------------------------------------------------------------------*/
    protected JBColor unreachableColor;
    protected JBColor edgesColor;
    protected JBColor errorColor;
    protected JBColor endOfFileColor;
    protected JBColor terminalNodeColor;
    protected JBColor terminalTextColor;
    protected JBColor selectedNodeColor;

    /*---- MOUSE --------------------------------------------------------------------------------*/
    private boolean mouseDown;
    private Point2D lastMousePos, currentMousePos, deltaMousePos;

    private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();
    protected JScrollPane scrollPane;
    private final boolean highlightUnreachedNodes;

    protected int minCellWidth;
    protected float edgesStrokeWidth;
    protected boolean autoscaling;
    protected Point2D offset;
    private long count;
    protected Tree selectedTreeNode;


    /**
     * Provides layout information based on font and text of a tree node.
     */
    public static class VariableExtentProvider implements NodeExtentProvider<Tree> {
        UberTreeViewer viewer;


        public VariableExtentProvider(UberTreeViewer viewer) {
            this.viewer = viewer;
        }


        @Override
        public double getWidth(Tree tree) {
            FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
            String s = viewer.getText(tree);
            int w = fontMetrics.stringWidth(s) + viewer.nodeWidthPadding * 2;

            // Do not use min size for terminals.
            if (tree instanceof TerminalNode) {
                return w;
            }

            return Math.max(w, viewer.minCellWidth);
        }


        @Override
        public double getHeight(Tree tree) {
            FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
            int h = fontMetrics.getHeight() + viewer.nodeHeightPadding * 2;
            String s = viewer.getText(tree);
            String[] lines = s.split("\n");
            //   System.out.println("getHeight(" + s + ") = " + h * lines.length + " lines=" + lines.length);
            return h + (lines.length - 1) * fontMetrics.getHeight();
        }
    }


    /**
     * Creates the UberTreeViewer component based on the given tree node.
     *
     * @param ruleNames               A list of rule names.
     * @param tree                    The root tree node of the diagram.
     * @param highlightUnreachedNodes If set unreached nodes are highlighted.
     */
    public UberTreeViewer(List<String> ruleNames, Tree tree, boolean highlightUnreachedNodes) {
        super(ruleNames, tree);
        this.highlightUnreachedNodes = highlightUnreachedNodes;

        this.highlightedNodes = new ArrayList<>();

        /* draw offset for diagram - needed to draw centered */
        offset = new Point2D.Double(0, 0);

        /* font setup */
        fontSize = 13;
        font = JBFont.regular().deriveFont((float) (fontSize));
        fontName = font.getFontName();

        /* edges setup */
        boolean useCurvedEdges = true;
        setUseCurvedEdges(useCurvedEdges);
        edgesColor = JBColor.BLACK;
        edgesStrokeWidth = 1.3f;

        /* color and shape setup */
        boxColor = JBColor.BLUE;
        terminalNodeColor = null;
        terminalTextColor = JBColor.BLACK;
        endOfFileColor = JBColor.LIGHT_GRAY;
        borderColor = null;
        arcSize = 9;
        minCellWidth = 100;
        gapBetweenLevels = 30;
        nodeHeightPadding = 6;
        nodeWidthPadding = 6;

        highlightedBoxColor = JBColor.PINK;
        selectedNodeColor = JBColor.PINK;
        unreachableColor = JBColor.orange;
        errorColor = JBColor.RED;
        textColor = JBColor.WHITE;

        autoscaling = true;

        // add handler for mouse events
        addMouseListener(this);
        addMouseMotionListener(this);

        mouseDown = false;
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


    @Override
    protected void paintEdges(Graphics g, Tree parent) {
        if (!getTree().isLeaf(parent)) {
            BasicStroke stroke = new BasicStroke(edgesStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            ((Graphics2D) g).setStroke(stroke);

            Rectangle2D.Double parentBounds = getBoundsOfNode(parent);
            double x1 = parentBounds.getCenterX();
            double y1 = parentBounds.getMaxY();

            g.setColor(edgesColor);

            for (Tree child : getTree().getChildren(parent)) {
                Rectangle2D.Double childBounds = getBoundsOfNode(child);
                double x2 = childBounds.getCenterX();
                double y2 = childBounds.getMinY();

                if (getUseCurvedEdges() && x1 != x2) {
                    double alpha = Math.abs(x1 - x2) * 0.1;
                    CubicCurve2D c = new CubicCurve2D.Double();
                    double ctrlx1 = x1;
                    double ctrly1 = y1 + alpha;
                    double ctrlx2 = x2;
                    double ctrly2 = y2 - alpha;
                    c.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
                    ((Graphics2D) g).draw(c);
                } else {
                    g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }

                paintEdges(g, child);
            }
        }
    }


    /**
     * Shortcut to scale handler.
     */
    protected void doAutoScale() {
        if (!hasTree()) return;

        Rectangle viewport = scrollPane.getViewportBorderBounds();

        double xRatio = (viewport.getWidth() - VIEWER_HORIZONTAL_MARGIN) / (treeLayout.getBounds().getWidth() + offset.getX());
        double yRatio = (viewport.getHeight() - VIEWER_HORIZONTAL_MARGIN) / (treeLayout.getBounds().getHeight() + offset.getY());

        // determine the smallest scale factor
        scale = Math.min(xRatio, yRatio);
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
        if (!hasTree()) return;

        double old = scale;

        if (autoscaling) {
            doAutoScale();
        }

        // clamp scale factor
        scale = Math.min(scale, MAX_SCALE_FACTOR);
        scale = Math.max(scale, MIN_SCALE_FACTOR);


        // update offset to center content
        updateOffset();

        // update just in case of changes to avoid performance issues
        if (scale != old)
            updatePreferredSize();
    }


    /**
     * Compute the horizontal offset for centered alignment.
     */
    private void updateOffset() {
        if (!hasTree()) return;

        Rectangle viewport = scrollPane.getViewportBorderBounds();

        // no offset if the size of the layout tree is bigger then the actual viewport
        if (viewport.getWidth() <= getScaledTreeSize().width) {
            offset.setLocation(0, VIEWER_HORIZONTAL_MARGIN / 2.);
            return;
        }


        double offs;
        offs = viewport.getWidth() / 2. - getScaledTreeSize().width / 2.;
        offs = offs * (1. / scale);
        offset.setLocation(offs, VIEWER_HORIZONTAL_MARGIN / 2.);
    }


    /**
     * Paint component.
     *
     * @param g Graphics context.
     * @see javax.swing.JComponent
     */
    @Override
    public void paint(Graphics g) {
        long time = System.nanoTime();

        ArrayList<String> buff = new ArrayList<>();

        buff.add("PARAMETER:\n");
        buff.add("parent: " + getParent().getWidth() + "x" + getParent().getHeight());


        if (treeLayout != null) {
            updateScaling();
            super.paint(g);
        } else {
            super.paint(g);
            return;
        }

        buff.add("fc&off: " + String.format("%.3f", scale) + " / " + String.format("%.3f", offset.getX()));
        buff.add("compon: " + getWidth() + "x" + getHeight());
        buff.add("tree  : " + treeLayout.getBounds().getWidth() + "x" + treeLayout.getBounds().getHeight());
        buff.add("tree N: " + String.format("%.3f", treeLayout.getBounds().getWidth() * scale) + "x" + String.format("%.3f", treeLayout.getBounds().getHeight() * scale));


        Point2D offsetText = new Point(0, 0);
        if (scrollPane != null) {
            offsetText.setLocation(scrollPane.getHorizontalScrollBar().getValue() / scale, scrollPane.getVerticalScrollBar().getValue() / scale);
            buff.add("scrollbars: " + scrollPane.getHorizontalScrollBar().getValue() + " : " + scrollPane.getVerticalScrollBar().getValue());
            buff.add("scrollbars: " + offsetText.getX() + " : " + offsetText.getY());
            buff.add("scroll val: " + String.format("%.3f", scrollPane.getHorizontalScrollBar().getMaximum() / scale) + " - " + String.format("%.3f",
                scrollPane.getVerticalScrollBar().getMaximum() / scale));
            buff.add("mouse     : " + (mouseDown ? "DOWN" : " UP "));
            buff.add("mouse pos : " + currentMousePos.toString().substring(14));
            buff.add("mouse last: " + lastMousePos.toString().substring(14));
            buff.add("mouse delt: " + deltaMousePos.toString().substring(14));
            buff.add("viewport  : " + scrollPane.getViewportBorderBounds().toString().substring(14));
        }

        buff.add("\n");

        Graphics2D g2 = (Graphics2D) g;

        // anti-alias the lines
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        //  g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);

        // this has to be turned on to proper render all text positions
        // if set to 'on' labels will not be layouted corrc
        g2.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_OFF);

        // Anti-alias the text
        g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);

        paintEdges(g, getTree().getRoot());

//        Rectangle2D bnd = treeLayout.getBounds();
//
//
//        g2.setColor(JBColor.RED.darker().darker().darker());
//        g2.fillRect((int) (bnd.getX() + offset.getX()), (int) (bnd.getY() + offset.getY()), (int) bnd.getWidth(), (int) bnd.getHeight());


        // paint the boxes
        for (Tree Tree : treeLayout.getNodeBounds().keySet()) {
            paintBox(g, Tree);
        }

        double delta = ((double) System.nanoTime() - time) / 1000000.;


        Font saved = g2.getFont();

        Font menlo = new Font("Menlo", Font.PLAIN, 14);

        g2.setFont(menlo.deriveFont((float) (13. * (1. / scale))).deriveFont(Font.BOLD));
        g2.setColor(JBColor.BLACK);

        buff.add("count: #" + count++);
        buff.add("time: " + String.format("%.3f", delta) + "ms");

        double y = 10. + offsetText.getY();
        for (String s : buff) {
            g2.drawString(s, (float) (10. + offsetText.getX()), (float) y);
            y += 18 * (1. / scale);
        }

        g2.setFont(saved);
    }


    /**
     * Draws a custom diagram box based on the tree nodes
     * layout information,
     *
     * @param g    Graphics context.
     * @param tree The tree node.
     */
    @Override
    protected void paintBox(Graphics g, Tree tree) {
        customPaintBox(g, tree);
    }


    /**
     * Custom diagram box paint method.
     *
     * @param g    Graphics Context.
     * @param tree Tree-Node to paint.
     */
    private void customPaintBox(Graphics g, Tree tree) {
        Rectangle2D.Double box = getBoundsOfNode(tree);
        Graphics2D g2 = (Graphics2D) g;

        // draw the box in the background
        boolean ruleFailedAndMatchedNothing = false;

        if (tree instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) tree;
            ruleFailedAndMatchedNothing =
                ctx.exception != null &&
                    ctx.stop != null &&
                    ctx.stop.getTokenIndex() < ctx.start.getTokenIndex();
        }

        Color color = boxColor;
        int boxRoundness = 1;

        if (tree instanceof ErrorNode || ruleFailedAndMatchedNothing)
            color = errorColor;


        if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();

            if (token.getText().equals("<EOF>")) {
                color = endOfFileColor;
                boxRoundness = 2;
            } else if (token.getType() == 0) {
                color = errorColor;
                boxRoundness = 3;
            } else {
                color = terminalNodeColor;
                boxRoundness = 0;
            }
        }


        /* selected node handled here */
        if (isSelectedTreeNode(tree)) {
            color = selectedNodeColor;

            BasicStroke stroke = new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2.setStroke(stroke);

            g2.setColor(selectedNodeColor.brighter());
            g2.drawRoundRect(
                (int) Math.round(box.x - 3),
                (int) Math.round(box.y - 3),
                (int) Math.round(box.width + 6),
                (int) Math.round(box.height + 6),
                arcSize * boxRoundness,
                arcSize * boxRoundness
            );
        }

        /* fill box */
        if (color != null) {
            g2.setColor(color);
            g2.fillRoundRect(
                (int) Math.round(box.x),
                (int) Math.round(box.y),
                (int) Math.round(box.width),
                (int) Math.round(box.height),
                arcSize * boxRoundness,
                arcSize * boxRoundness
            );
        }

        /* box border */
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(
                (int) Math.round(box.x),
                (int) Math.round(box.y),
                (int) Math.round(box.width),
                (int) Math.round(box.height),
                arcSize * boxRoundness,
                arcSize * boxRoundness
            );
        }

        // ---------------- PAINT LABELS AND TEXT -------------------------------
        g2.setFont(font);
        g2.setColor(textColor);

        if (tree.getParent() == null) {
            g2.setColor(JBColor.WHITE);
            g2.setFont(font.deriveFont(Font.BOLD).deriveFont((float) fontSize + 4));
        }

        if (tree instanceof TerminalNode) {
            g2.setFont(font.deriveFont(Font.BOLD).deriveFont((float) fontSize + 5));
            g2.setColor(terminalTextColor);
        }

        String s = getText(tree);
        FontMetrics m = g2.getFontMetrics(g2.getFont());

        float y = (float) (box.y + box.height / 2. - m.getHeight() / 2. + m.getAscent());
        float x = (float) (box.x + box.width / 2. - m.stringWidth(s) / 2.);

        g2.drawString(s, x, y);
    }


    /**
     * Returns the bounds of a tree node including the offset vector.
     *
     * @param node The tree node.
     * @return Bounds as {@code Rectangle2D}.
     */
    @Override
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
    @Override
    protected String getText(Tree tree) {
        String s = treeTextProvider.getText(tree);
        s = Utils.escapeWhitespace(s.trim(), false);
        return s;
    }


    /**
     * Returns the dimension of the tree layout
     *
     * @return
     */
    private Dimension getScaledTreeSize() {
        Dimension scaledTreeSize = treeLayout.getBounds().getBounds().getSize();

        return new Dimension(
            (int) Math.round(scaledTreeSize.width * scale),
            (int) Math.round(scaledTreeSize.height * scale + VIEWER_HORIZONTAL_MARGIN)
        );
    }


    /**
     * Checks if the scaled tree graphics exceeds the visible viewport.
     *
     * @return True if tree-view > scrollbar dimension.
     */
    protected boolean treeExceedsViewport() {
        Rectangle viewport = scrollPane.getViewportBorderBounds();
        return getScaledTreeSize().width > viewport.getWidth() ||
            getScaledTreeSize().height > viewport.getHeight();
    }


    /**
     * Update the component's size based on the tree layout size.
     */
    protected void updatePreferredSize() {
        setPreferredSize(getScaledTreeSize());

        if (getParent() != null)
            getParent().revalidate();

        repaint();
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
    @Override
    public void text(Graphics g, String s, int x, int y) {
        // s = Utils.escapeWhitespace(s, false);
        g.drawString(s, x, y);
    }


    /**
     * Creates a tree layout by out of a tree node.
     *
     * @param root The root node of the tree.
     */
    @Override
    public void setTree(Tree root) {
        if (root != null) {
            treeLayout = new TreeLayout<>(
                getTreeLayoutAdaptor(root),
                new VariableExtentProvider(this),
                new DefaultConfiguration<>(gapBetweenLevels, gapBetweenNodes),
                true
            );

            // Let the UI display this new AST.
            updatePreferredSize();
        } else {
            treeLayout = null;
            repaint();
        }
    }


    /**
     * Test if a valid tree layout has been assigned.
     *
     * @return True if assigned.
     */
    public boolean hasTree() {
        return treeLayout != null;
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

            // raise event for all selection listeners
            for (ParsingResultSelectionListener listener : selectionListeners) {
                listener.onParserRuleSelected(node);
            }

            Rectangle2D nodeBounds = getBoundsOfNode(node);
            Rectangle marginBox = new Rectangle(
                (int) Math.round(nodeBounds.getX() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getY() * scale - SCROLL_VIEWPORT_MARGIN),
                (int) Math.round(nodeBounds.getWidth() * scale + SCROLL_VIEWPORT_MARGIN * 2),
                (int) Math.round(nodeBounds.getHeight() * scale + SCROLL_VIEWPORT_MARGIN * 2)
            );

            scrollRectToVisible(marginBox);
        }

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
     * @param p
     * @return
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
        if (selectedTreeNode == null) return false;
        return tree == selectedTreeNode;
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


    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }


    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
            mouseDown = true;
            lastMousePos = mouseEvent.getPoint();
            testForNodeSelection(mouseEvent.getPoint());
        }
    }


    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (treeLayout == null) return;

        mouseDown = false;
        deltaMousePos.setLocation(0, 0);

        // reset cursor
        if (!locationHitsNode(mouseEvent.getPoint())) {
            setCursor(DEFAULT_CURSOR);
        }

        repaint();
    }


    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }


    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }


    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (treeLayout == null) return;

        setCursor(DRAG_CURSOR);
        deltaMousePos.setLocation(lastMousePos.getX() - mouseEvent.getX(), lastMousePos.getY() - mouseEvent.getY());

        if (treeExceedsViewport()) {
            int hval = scrollPane.getHorizontalScrollBar().getValue();
            int vval = scrollPane.getVerticalScrollBar().getValue();

            scrollPane.getHorizontalScrollBar().setValue(hval + (int) (deltaMousePos.getX()));
            scrollPane.getVerticalScrollBar().setValue(vval + (int) (deltaMousePos.getY()));

            repaint();
        }
    }


    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (treeLayout == null) return;

        currentMousePos = mouseEvent.getPoint();
        // check if there is a node under the mouse cursor
        if (locationHitsNode(currentMousePos)) {
            setCursor(SELECT_CURSOR);
        } else {
            setCursor(DEFAULT_CURSOR);
        }
        repaint();
    }
}
