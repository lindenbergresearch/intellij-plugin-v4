package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
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
    private final PropertiesTableModelModel propertiesTableModelModel;
    
    
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
        //   propertiesTable.setBackground(DefaultStyles.getConsoleBackground());
        propertiesTable.setFont(DefaultStyles.SMALL_FONT);
        
        var scrollPane = new JBScrollPane(
            propertiesTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        
        // AllIcons.Debugger.Watch,
        var caption = new JLabel(
            "Object Explorer",
            AllIcons.Debugger.Watch,
            SwingConstants.CENTER
        );
        
        // caption.setFont(DefaultStyles.REGULAR_FONT);
        caption.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        
        add(caption, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    
    /**
     * Collects common information to be shown in object explorer.
     *
     * @param infoLabel {@link JInfoLabel} instance from viewer
     *                  holding the current common parsing data.
     */
    public void setInfoLabel(JInfoLabel infoLabel) {
        clear(); // cleanup properties panel
        
        for (var infoLabelElement : infoLabel.getContent().values()) {
            addProperty(infoLabelElement.getLabel(), infoLabelElement.getDisplayText());
        }
    }
    
    
    /**
     * Collects information to be shown in object explorer relating selected tree-nodes.
     *
     * @param tree                 The parse-tree node to examine.
     * @param altLabelTextProvider The current used Text-provider.
     */
    public void setTreeNode(ParseTree tree, AltLabelTextProvider altLabelTextProvider) {
        clear(); // cleanup properties panel
        
        
        /* collect properties for terminals */
        if (tree instanceof TerminalNodeImpl) {
            var ctx = (TerminalNodeImpl) tree;
            var name = ctx.getClass().getSimpleName();
            var token = ctx.getSymbol();
            var text = ctx.getText();
            
            var length =
                ctx.getPayload().getStartIndex() < 0 || ctx.getPayload().getStopIndex() < 0 ? -1 :
                    ctx.getPayload().getStopIndex() - ctx.getPayload().getStartIndex() + 1;
            
            String parent;
            
            if (ctx.parent != null) {
                parent = altLabelTextProvider.getRule(ctx.getParent()).name;
            } else parent = "-";
            
            addProperty("class", '<' + name + '>');
            addProperty("parent-rule", '[' + parent + ']');
            addProperty("symbol", altLabelTextProvider.getSymbolicTokenName(token));
            addProperty("label", '"' + altLabelTextProvider.getText(tree) + '"');
            addProperty("text", '\'' + text + '\'');
            addProperty("length", length);
            addProperty("range", ctx.getSourceInterval());
            addProperty("token", token);
            
            //  addProperty("terminal", token.getText());
            addProperty("position", "[" + token.getLine() + ':' + token.getCharPositionInLine() + ']');
            addProperty("token id", token.getType());
            addProperty("token channel", token.getChannel());
            
            return;
        }
        
        /* --------------------------------------------------------------------- */
        
        /* collect properties for rule nodes */
        if (tree instanceof PreviewInterpreterRuleContext) {
            var ctx = (PreviewInterpreterRuleContext) tree;
            var name = ctx.getClass().getSimpleName();
            var ruleName = altLabelTextProvider.getRule(tree).name;
            var text = ctx.getText();
            var label = altLabelTextProvider.getRuleLabel(tree);
            var subTree = ctx.toString();
            var startToken = ctx.getStart();
            var stopToken = ctx.getStop();
            
            var childNum = ctx.getChildCount();
            var altNum = ctx.getAltNumber();
            var outerAltNum = ctx.getOuterAltNum();
            var depth = ctx.depth();
            
            var length =
                ctx.start == null || ctx.stop == null || ctx.start.getStartIndex() < 0 || ctx.stop.getStopIndex() < 0 ? -1 :
                    ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1;
            
            String parent;
            
            if (ctx.parent != null) {
                parent = altLabelTextProvider.getRule(ctx.getParent()).name;
            } else parent = "-";
            
            var hasException =
                ctx.exception != null ?
                    ctx.exception.getClass().getSimpleName() :
                    "-";
            
            var exceptionMsg =
                (ctx.exception != null) && ctx.exception.getMessage() != null ?
                    ctx.exception.getMessage() :
                    "-";
            
            
            addProperty("class", '<' + name + '>');
            addProperty("rule-name", '[' + ruleName + ']');
            addProperty("parent-rule", '[' + parent + ']');
            addProperty("label", label);
            addProperty("text", '"' + text + '"');
            addProperty("length", length);
            addProperty("child-count", childNum);
            addProperty("alternatives", altNum);
            addProperty("outer-alternatives", outerAltNum);
            addProperty("tree-level", depth);
            addProperty("toString()", subTree);
            
            addProperty("exception", hasException);
            addProperty("message", exceptionMsg);
            
            addProperty("range", ctx.getSourceInterval());
            
            if (startToken != null) {
                addProperty("range start", "line=" + startToken.getLine() + " char=" + startToken.getCharPositionInLine());
                addProperty("start-token", startToken);
            } else {
                addProperty("range start", "-1");
                addProperty("start-token", "-");
            }
            
            if (stopToken != null) {
                addProperty("range end", "line=" + stopToken.getLine() + " char=" + stopToken.getCharPositionInLine());
                addProperty("stop-token", stopToken);
            } else {
                addProperty("range end", "-1");
                addProperty("stop-token", "-");
            }
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
