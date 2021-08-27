package org.antlr.intellij.plugin.preview;

import com.intellij.ui.DarculaColors;
import com.intellij.ui.Gray;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static java.awt.RenderingHints.*;


/**
 *
 */
public class UberTreeViewer extends TreeViewer {

    private final boolean highlightUnreachedNodes;
    private final boolean useCurvedEdges;

    protected JBColor unreachableColor;
    protected JBColor edgesColor;
    protected JBColor errorColor;
    protected JBColor endOfFileColor;
    protected JBColor terminalNodeColor;

    protected int minCellWidth;
    private float edgesStrokeWidth;


    /**
     *
     */
    public static class VariableExtentProvide implements NodeExtentProvider<Tree> {
        UberTreeViewer viewer;


        public VariableExtentProvide(UberTreeViewer viewer) {
            this.viewer = viewer;
        }


        @Override
        public double getWidth(Tree tree) {
            FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
            String s = viewer.getText(tree);
            int w = fontMetrics.stringWidth(s) + viewer.nodeWidthPadding * 2;
            //   System.out.println("getWidth(" + s + ") = " + w);
            return Math.max(w, viewer.minCellWidth);
        }


        @Override
        public double getHeight(Tree tree) {
            FontMetrics fontMetrics = viewer.getFontMetrics(viewer.font);
            int h = fontMetrics.getHeight() + viewer.nodeHeightPadding * 2;
            String s = viewer.getText(tree);
            String[] lines = s.split("\n");
            //   System.out.println("getHeight(" + s + ") = " + h * lines.length + " lines=" + lines.length);

            return h * lines.length;
        }
    }


    public UberTreeViewer(List<String> ruleNames, Tree tree, boolean highlightUnreachedNodes) {
        super(ruleNames, tree);
        this.highlightUnreachedNodes = highlightUnreachedNodes;

        fontSize = 13;
        font = JBFont.regular().deriveFont((float) (fontSize));
        fontName = font.getFontName();

        useCurvedEdges = true;
        setUseCurvedEdges(useCurvedEdges);
        edgesColor = JBColor.BLACK;
        edgesStrokeWidth = 1.4f;

        boxColor = JBColor.BLUE;
        terminalNodeColor = JBColor.ORANGE;
        endOfFileColor = JBColor.DARK_GRAY;
        borderColor = null;
        arcSize = 9;
        minCellWidth = 100;
        gapBetweenLevels = 30;
        nodeHeightPadding = 12;
        nodeWidthPadding = 7;

        unreachableColor = JBColor.orange;
        errorColor = JBColor.RED;
        textColor = JBColor.WHITE;

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    @Override
    protected void paintEdges(Graphics g, Tree parent) {
        if (!getTree().isLeaf(parent)) {
            BasicStroke stroke = new BasicStroke(edgesStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            ((Graphics2D) g).setStroke(stroke);

            Rectangle2D.Double parentBounds = getBoundsOfNode(parent);
            double x1 = parentBounds.getCenterX();
            double y1 = parentBounds.getMaxY() - 3;

            g.setColor(edgesColor);

            for (Tree child : getTree().getChildren(parent)) {
                Rectangle2D.Double childBounds = getBoundsOfNode(child);
                double x2 = childBounds.getCenterX();
                double y2 = childBounds.getMinY();

                if (getUseCurvedEdges() && x1 != x2) {
                    CubicCurve2D c = new CubicCurve2D.Double();
                    double ctrlx1 = x1;
                    double ctrly1 = y1 + 15;
                    double ctrlx2 = x2;
                    double ctrly2 = y2 - 15;
                    c.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
                    ((Graphics2D) g).draw(c);
                } else {
                    g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }

                paintEdges(g, child);
            }
        }
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (treeLayout == null) return;

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

        // paint the boxes
        for (Tree Tree : treeLayout.getNodeBounds().keySet()) {
            paintBox(g, Tree);
        }
    }


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

        if (isHighlighted(tree)) color = highlightedBoxColor;
        else if (tree instanceof ErrorNode || ruleFailedAndMatchedNothing) color = errorColor;
        else if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            if (token.getText().equals("<EOF>")) {
                color = endOfFileColor;
                boxRoundness = 3;
            } else {
                color = terminalNodeColor;
                boxRoundness = 6;
            }

        } else if (boxColor != null) color = boxColor;
        else color = null;

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

        if (tree instanceof ErrorNode || ruleFailedAndMatchedNothing) {
            g.setColor(errorColor);
        } else {
            g.setColor(textColor);
        }

        String s = getText(tree);
        String[] lines = s.split("\n");
        FontMetrics m = getFontMetrics(font);
        int y = (int) Math.round(box.y + m.getAscent() + m.getLeading() + nodeHeightPadding);

        for (String line : lines) {
            int strWidth = m.stringWidth(line);
            int x = (int) box.x + (int) Math.round(box.width / 2. - strWidth / 2.);

            text(g, line, x, y);
            y += m.getHeight();
        }
    }


    private Dimension getScaledTreeSize() {
        Dimension scaledTreeSize = treeLayout.getBounds().getBounds().getSize();
        scaledTreeSize = new Dimension(
                (int) Math.round(scaledTreeSize.width * scale),
                (int) Math.round(scaledTreeSize.height * scale)
        );


        return scaledTreeSize;
    }


    private void updatePreferredSize() {
        setPreferredSize(getScaledTreeSize());
        invalidate();
        if (getParent() != null) {getParent().validate();}
        repaint();
    }


    @Override
    public void text(Graphics g, String s, int x, int y) {
        //    System.out.println("drawing '" + s + "' @ " + x + "," + y);
        s = Utils.escapeWhitespace(s, false);
        g.drawString(s, x, y);
    }


    @Override
    public void setTree(Tree root) {
        if (root != null) {
            treeLayout = new TreeLayout<>(
                    getTreeLayoutAdaptor(root),
                    new VariableExtentProvide(this),
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


    public boolean hasTree() {
        return treeLayout != null;
    }
}
