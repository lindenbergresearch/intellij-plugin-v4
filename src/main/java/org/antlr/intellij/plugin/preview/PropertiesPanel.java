package org.antlr.intellij.plugin.preview;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Panel for displaying 2 column mode
 * properties in the form: key - value
 */
public class PropertiesPanel extends JPanel {
    private JBTable propertiesTable;
    private PropertiesTableModelModel propertiesTableModelModel;
    private JBScrollPane scrollPane;
    private JLabel caption;
    private AltLabelTextProvider altLabelTextProvider;
    
    
    public PropertiesPanel(LayoutManager layout, Border border) {
        super(layout, true);
        setBorder(border);
        
        propertiesTableModelModel =
            new PropertiesTableModelModel("Property", "Value");
        
        propertiesTable = new JBTable(propertiesTableModelModel);
        propertiesTable.setFillsViewportHeight(true);
        propertiesTable.getEmptyText().appendLine("Select a node to display");
        propertiesTable.getEmptyText().appendLine("its properties.");
        //  propertiesTable.setStriped(true);
        propertiesTable.setShowVerticalLines(true);
        propertiesTable.setBackground(DefaultStyles.getConsoleBackground());
        propertiesTable.setFont(DefaultStyles.VERY_SMALL_FONT);
        
        scrollPane = new JBScrollPane(
            propertiesTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        
        caption = new JLabel(
            "Node Explorer",
            // AllIcons.Debugger.Watch,
            SwingConstants.CENTER
        );
        
        caption.setFont(DefaultStyles.SMALL_FONT.deriveFont(5.f));
        caption.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        
        add(caption, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    
    /**
     * Collects information to be shown in object explorer.
     *
     * @param tree                 The parse-tree node to examine.
     * @param altLabelTextProvider The current used Text-provider.
     */
    public void setTreeNode(ParseTree tree, AltLabelTextProvider altLabelTextProvider) {
        clear(); // cleanup properties panel
        
        
        /* collect properties for terminals */
        if (tree instanceof TerminalNodeImpl) {
            TerminalNodeImpl ctx = (TerminalNodeImpl) tree;
            String name = ctx.getClass().getSimpleName();
            Token token = ctx.getSymbol();
            String text = ctx.getText();
            
            String parent;
            
            if (ctx.parent != null) {
                parent = altLabelTextProvider.getRule(ctx.getParent()).name;
            } else parent = "-";
            
            addProperty("class", '<' + name + '>');
            addProperty("parent-rule", '[' + parent + ']');
            addProperty("symbol", altLabelTextProvider.getTokenName(token));
            addProperty("label", '«' + altLabelTextProvider.getText(tree) + '»');
            addProperty("token", token);
            addProperty("text", '\'' + text + '\'');
            
            //  addProperty("terminal", token.getText());
            addProperty("position", "[" + token.getLine() + ':' + token.getCharPositionInLine() + ']');
            addProperty("token id", token.getType());
            addProperty("token channel", token.getChannel());
            addProperty("start index", token.getStartIndex());
            addProperty("stop index", token.getStopIndex());
            
            return;
        }
        
        /* --------------------------------------------------------------------- */
        
        /* collect properties for rule nodes */
        if (tree instanceof PreviewInterpreterRuleContext) {
            PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext) tree;
            String name = ctx.getClass().getSimpleName();
            String ruleName = altLabelTextProvider.getRule(tree).name;
            String text = ctx.getText();
            String label = altLabelTextProvider.getRuleLabel(tree);
            String subTree = ctx.toStringTree();
            Token startToken = ctx.getStart();
            Token stopToken = ctx.getStop();
            
            int childNum = ctx.getChildCount();
            int altNum = ctx.getAltNumber();
            int outerAltNum = ctx.getOuterAltNum();
            int depth = ctx.depth();
            
            String parent;
            
            if (ctx.parent != null) {
                parent = altLabelTextProvider.getRule(ctx.getParent()).name;
            } else parent = "-";
            
            String hasException =
                ctx.exception != null ?
                    ctx.exception.getClass().getSimpleName() :
                    "-";
            
            String exceptionMsg =
                (ctx.exception != null) && ctx.exception.getMessage() != null ?
                    ctx.exception.getMessage() :
                    "-";
            
            
            addProperty("class", '<' + name + '>');
            addProperty("rule-name", '[' + ruleName + ']');
            addProperty("parent-rule", '[' + parent + ']');
            addProperty("label", label);
            addProperty("text", '\u00AB' + text + '\u00BB');
            addProperty("child-count", childNum);
            addProperty("alternatives", altNum);
            addProperty("outer-alternatives", outerAltNum);
            addProperty("depth", depth);
            addProperty("sub-tree", subTree);
            
            addProperty("exception", hasException);
            addProperty("message", exceptionMsg);
            
            
            addProperty("range start", "line=" + startToken.getLine() + " char=" + startToken.getCharPositionInLine());
            addProperty("range end", "line=" + stopToken.getLine() + " char=" + stopToken.getCharPositionInLine());
            
            addProperty("start-token", startToken);
            addProperty("stop-token", stopToken);
        }
        
        
    }
    
    
    public void addProperty(String name, Object value) {
        propertiesTableModelModel.getProperties().add(new Pair<>(name, value));
    }
    
    
    public void clear() {
        propertiesTableModelModel.clear();
    }
    
    
    public JBTable getPropertiesTable() {
        return propertiesTable;
    }
    
    
    public void setPropertiesTable(JBTable propertiesTable) {
        this.propertiesTable = propertiesTable;
    }
}
