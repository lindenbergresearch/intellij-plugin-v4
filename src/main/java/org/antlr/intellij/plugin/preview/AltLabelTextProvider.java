package org.antlr.intellij.plugin.preview;

import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;

import java.util.List;
import java.util.Map;

/**
 * Provides formatted text of a given Tree-Node.
 */
public class AltLabelTextProvider implements TreeTextProvider {
    // text displayed for EOF node
    public static final String EOF_LABEL = "<EOF>\nend-of-file";
    
    // max length of token label before cut shortened
    public static final int MAX_TOKEN_LENGTH = 6;
    
    // alt label prefix
    public static final String ALT_LABEL_TEXT = "\u2022";
    
    // ...
    public static final String SHORTEN_LABEL_TEXT = "\u2026";
    
    // shorthand
    public static final String NL = System.lineSeparator();
    
    // prefix for rule label
    public static final char RULE_LABEL_PREFIX = '#';
    
    // text used if name, text or symbol is not available
    public static final String NOT_PRESENT_TEXT = "-";
    
    
    // use compact labels
    private boolean compact = false;
    
    
    private final Parser parser;
    private final Grammar g;
    
    
    /* --------------------------------------------------------------------- */
    
    
    /**
     * Constructs a text-provider.
     *
     * @param parser Parser.
     * @param g      Grammar.
     */
    public AltLabelTextProvider(Parser parser, Grammar g) {
        this.parser = parser;
        this.g = g;
    }
    
    
    /**
     * Get alternatives labels.
     *
     * @param r Rule to examine for alternatives.
     * @return Array of label strings.
     */
    public String[] getAltLabels(Rule r) {
        String[] altLabels = null;
        Map<String, List<Pair<Integer, AltAST>>> altLabelsMap = r.getAltLabels();
        
        if (altLabelsMap != null) {
            altLabels = new String[r.getOriginalNumberOfAlts() + 1];
            
            for (String altLabel : altLabelsMap.keySet()) {
                List<Pair<Integer, AltAST>> pairs = altLabelsMap.get(altLabel);
                
                for (Pair<Integer, AltAST> pair : pairs) {
                    altLabels[pair.a] = altLabel;
                }
            }
        }
        
        return altLabels;
    }
    
    
    /**
     * Returns the label of a parse-tree node, if one is set.
     *
     * @param node Node to examine.
     * @return Either the label prefixed with '#' or '-'.
     */
    public String getRuleLabel(Tree node) {
        if (node instanceof PreviewInterpreterRuleContext) {
            String[] altLabels = getAltLabels(getRule(node));
            int outerAltNum = getOuterAltNum(node);
            
            if (hasRuleLabel(node)) {
                return RULE_LABEL_PREFIX + altLabels[outerAltNum];
            }
        }
        
        return NOT_PRESENT_TEXT;
    }
    
    
    /**
     * Checks if a given tree-node is a rule and have a label.
     *
     * @param node Tree-node.
     * @return True if is a rule and has a label.
     */
    private boolean hasRuleLabel(Tree node) {
        if (node instanceof PreviewInterpreterRuleContext) {
            String[] altLabels = getAltLabels(getRule(node));
            int outerAltNum = getOuterAltNum(node);
            
            if (altLabels != null) {
                return
                    outerAltNum >= 0 &&
                    outerAltNum < altLabels.length;
            }
        }
        
        return false;
    }
    
    
    /**
     * Returns the formatted text of the given tree-node.
     *
     * @param node Tree-node.
     * @return Formatted string.
     */
    @Override
    public String getText(Tree node) {
        // terminal node
        if (node instanceof TerminalNode) {
            return getTokenLabel(node);
        }
        
        String text = NOT_PRESENT_TEXT;
        if (node instanceof PreviewInterpreterRuleContext) {
            int originalAltNums = getRule(node).getOriginalNumberOfAlts();
            int outerAltNum = getOuterAltNum(node);
            text = '[' + getRule(node).name + ']';
            
            if (hasRuleLabel(node)) {
                if (compact) text = getRuleLabel(node);
                else text += NL + getRuleLabel(node);
            }
            
            if (originalAltNums > 1) {
                text += ALT_LABEL_TEXT + outerAltNum;
            }
        }
        
        return text;
    }
    
    
    /**
     * Returns the symbolic name of a token.
     *
     * @param token Token to examine.
     * @return Name as string.
     */
    public String getSymbolicTokenName(Token token) {
        String symName = parser.getVocabulary().getSymbolicName(token.getType());
        return symName == null ? NOT_PRESENT_TEXT : symName;
    }
    
    
    /**
     * Returns the formatted label of a given token.
     *
     * @param node tree-node.
     * @return Label as string.
     */
    public String getTokenLabel(Tree node) {
        Token token = ((TerminalNode) node).getSymbol();
        String text = token.getText();
        String symName = parser.getVocabulary().getSymbolicName(token.getType());
        
        // prevent node label getting to long
        if (text.length() > MAX_TOKEN_LENGTH)
            text = text.substring(0, MAX_TOKEN_LENGTH) + SHORTEN_LABEL_TEXT;
        
        // not part of the actual match, it is resync
        if (node instanceof ErrorNodeImpl) {
            text = symName == null ?
                '\'' + text + '\'' :
                symName + ": " + '\'' + text + '\'';
            
            return "<resync>" + NL + text;
        }
        
        if (text.equals("<EOF>")) return EOF_LABEL;
        if (symName == null) return text;
        
        if (compact) return text;
        
        return symName + NL + '\'' + text + '\'';
    }
    
    
    /**
     * Returns the associated rule of the given tree-node.
     *
     * @param node Tree-node.
     * @return The Rule.
     */
    public Rule getRule(Tree node) {
        PreviewInterpreterRuleContext inode =
            (PreviewInterpreterRuleContext) node;
        return g.getRule(inode.getRuleIndex());
    }
    
    
    /**
     * The predicted outermost alternative for the rule associated with this context object.
     * If left recursive, the true original outermost alternative is returned.
     *
     * @param node Tree-node to examine.
     * @return Alt num.
     */
    public int getOuterAltNum(Tree node) {
        return ((PreviewInterpreterRuleContext) node).getOuterAltNum();
    }
    
    
    /**
     * Check if viewer is in compact mode.
     *
     * @return True if in compact mode.
     */
    public boolean isCompact() {
        return compact;
    }
    
    
    /**
     * Enable or disable compact mode of the viewer.
     *
     * @param compact Enable flag.
     */
    public void setCompact(boolean compact) {
        this.compact = compact;
    }
    
    
    public Parser getParser() {
        return parser;
    }
    
    
    public Grammar getGrammar() {
        return g;
    }
}
