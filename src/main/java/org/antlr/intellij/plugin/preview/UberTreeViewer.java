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

import java.awt.*;
import java.awt.event.MouseEvent;
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
public class UberTreeViewer extends TreeViewer {
    public final static double MAX_SCALE_FACTOR = 1.66;
    public final static double MIN_SCALE_FACTOR = 0.1;
    public static final int VIEWER_HORIZONTAL_MARGIN = 26;

    private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();

    private final boolean highlightUnreachedNodes;

    protected JBColor unreachableColor;
    protected JBColor edgesColor;
    protected JBColor errorColor;
    protected JBColor endOfFileColor;
    protected JBColor terminalNodeColor;
    protected JBColor selectedNodeColor;

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
        terminalNodeColor = JBColor.ORANGE;
        endOfFileColor = JBColor.DARK_GRAY;
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

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        autoscaling = true;
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
        // flag already set
        if (autoscaling) return;

        autoscaling = true;
        updateScaling(0);
    }


    /**
     * Increment zoom level by delta factor.
     * Disables auto-scaling.
     *
     * @param delta Delta factor. (0.1 = 10% etc.)
     */
    protected void setRelativeScaling(double delta) {
        autoscaling = false;
        updateScaling(this.scale += delta);
    }


    /**
     * Set new scale level and reset auto-scaling.
     *
     * @param newScale New scale as factor. (0.1 = 10% etc.)
     */
    protected void setScaleLevel(double newScale) {
        autoscaling = false;
        updateScaling(newScale);
    }


    /**
     * Computes the correct scale-factor for proper zoom to fit content.
     * Zooming are limited to: 10% - 166%.
     */
    protected void updateScaling(double newScale) {
        double factor, offs;

        if (autoscaling) {
            double xRatio = (double) (getParent().getWidth() - VIEWER_HORIZONTAL_MARGIN) / (treeLayout.getBounds().getWidth() + offset.getX());
            double yRatio = (double) (getParent().getHeight() - VIEWER_HORIZONTAL_MARGIN) / (treeLayout.getBounds().getHeight() + offset.getY());

            factor = Math.min(xRatio, yRatio);
        } else {
            factor = newScale;
        }

        // clamp scale factor
        factor = Math.min(factor, MAX_SCALE_FACTOR);
        factor = Math.max(factor, MIN_SCALE_FACTOR);
        scale = factor;

        offs = (double) getParent().getWidth() / 2 - treeLayout.getBounds().getWidth() * scale / 2;
        offs = offs * (1. / scale);
        offset.setLocation(offs, VIEWER_HORIZONTAL_MARGIN / 2.);

        updatePreferredSize();
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
            if (autoscaling) doAutoScale();
            super.paint(g);
        } else {
            super.paint(g);
            return;
        }

        buff.add("fc&off: " + String.format("%.3f", scale) + " / " + String.format("%.3f", offset.getX()));
        buff.add("compon: " + getWidth() + "x" + getHeight());
        buff.add("tree  : " + treeLayout.getBounds().getWidth() + "x" + treeLayout.getBounds().getHeight());
        buff.add("tree N: " + String.format("%.3f", treeLayout.getBounds().getWidth() * scale) + "x" + String.format("%.3f", treeLayout.getBounds().getHeight() * scale));
        buff.add("\n");


        Graphics2D g2 = (Graphics2D) g;

        // anti-alias the lines
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        //    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);

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

        int y = 10;
        for (String s : buff) {
            g2.drawString(s, 10, y);
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

        Rectangle2D.Double box = getBoundsOfNode(tree);

        if (tree instanceof PreviewInterpreterRuleContext) {
            PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext) tree;

            if (highlightUnreachedNodes && !ctx.reached) {
                g.setColor(unreachableColor);
                g.drawRoundRect(
                        (int) Math.round(box.x),
                        (int) Math.round(box.y),
                        (int) Math.round(box.width - 1),
                        (int) Math.round(box.height - 1),
                        arcSize,
                        arcSize
                );
            }
        }
    }


    /**
     * Custom diagram box paint method.
     *
     * @param g    Graphics Context.
     * @param tree Tree-Node to paint.
     */
    private void customPaintBox(Graphics g, Tree tree) {
        Rectangle2D.Double box = getBoundsOfNode(tree);

        // draw the box in the background
        boolean ruleFailedAndMatchedNothing = false;

        if (tree instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) tree;
            ruleFailedAndMatchedNothing = ctx.exception != null && ctx.stop != null && ctx.stop.getTokenIndex() < ctx.start.getTokenIndex();
        }

        Color color;
        int boxRoundness = 1;


        if (tree instanceof ErrorNode || ruleFailedAndMatchedNothing) color = errorColor;
        else if (tree instanceof TerminalNode) {
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

        } else if (boxColor != null) color = boxColor;
        else color = null;


        /* selected node handled here */
        if (isSelectedTreeNode(tree)) {
            color = selectedNodeColor;

            BasicStroke stroke = new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            ((Graphics2D) g).setStroke(stroke);

            g.setColor(selectedNodeColor.brighter());
            g.drawRoundRect(
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
            g.setColor(color);
            g.fillRoundRect(
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
            g.setColor(borderColor);
            g.drawRoundRect(
                    (int) Math.round(box.x),
                    (int) Math.round(box.y),
                    (int) Math.round(box.width),
                    (int) Math.round(box.height),
                    arcSize * boxRoundness,
                    arcSize * boxRoundness
            );
        }

        // ---------------- PAINT LABELS AND TEXT -------------------------------

//        if (tree instanceof ErrorNode || ruleFailedAndMatchedNothing) {
//            g.setColor(errorColor);
//        } else {
//            g.setColor(textColor);
//        }

        g.setFont(font);
        g.setColor(textColor);

        if (tree.getParent() == null) {
            g.setColor(JBColor.WHITE);
            g.setFont(font.deriveFont(Font.BOLD).deriveFont((float) fontSize));
        }

//        if (tree instanceof TerminalNode) {
//            g.setFont(font.deriveFont(Font.ITALIC).deriveFont((float) fontSize ));
//        }
        //s = Utils.escapeWhitespace(s, true);


        String s = getText(tree);
        FontMetrics m = getFontMetrics(getFont());
        int y = (int) Math.round(box.y + box.height / 2. - m.getHeight() / 2. + m.getAscent());
        int x = (int) Math.round(box.x + box.width / 2. - m.stringWidth(s) / 2.);

        text(g, s, x, y);
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

        Rectangle2D.Double newBounds = new Rectangle2D.Double(
                bounds.x + offset.getX(),
                bounds.y + offset.getY(),
                bounds.width, bounds.height
        );

        return newBounds;
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
        s = Utils.escapeWhitespace(s, false);
        return s;
    }


    /**
     * Returns the dimension of the tree layout
     *
     * @return
     */
    private Dimension getScaledTreeSize() {
        Dimension scaledTreeSize = treeLayout.getBounds().getBounds().getSize();
        scaledTreeSize = new Dimension(
                (int) Math.round(scaledTreeSize.width * scale),
                (int) Math.round(scaledTreeSize.height * scale)
        );


        return scaledTreeSize;
    }


    /**
     * Update the component's size based on the tree layout size.
     */
    private void updatePreferredSize() {
        setPreferredSize(getScaledTreeSize());
        invalidate();

        if (getParent() != null)
            getParent().validate();

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
     * Handle mouse events and may raise an event at the {@code ParsingResultSelectionListener}
     * to notify all subscriber.
     *
     * @param e Mouse Event
     * @see ParsingResultSelectionListener
     */
    protected void handleMouseEvent(MouseEvent e) {
        // do nothing on an invalid tree
        if (treeLayout == null || treeLayout.getLevelCount() == 0) return;

        // always clear on click
        setSelectedTreeNode(null);

        for (Tree tree : treeLayout.getNodeBounds().keySet()) {
            Rectangle2D.Double box = getBoundsOfNode(tree);

            if (box.contains(e.getX() / scale, e.getY() / scale)) {
                // set selected node
                setSelectedTreeNode(tree);

                // raise event for all selection listeners
                for (ParsingResultSelectionListener listener : selectionListeners) {
                    listener.onParserRuleSelected(tree);
                }
            }
        }
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

}
