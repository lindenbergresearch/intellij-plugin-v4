package org.antlr.intellij.plugin.preview;

import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
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
    public static final String EOF_LABEL = "<EOF>";

    // maximum length of a nodes label before shortened with '...'
    public static final int MAX_TOKEN_LENGTH = 7;
    public static final String ALT_LABEL_TEXT = " ⁕";
    public static final String SHORTEN_LABEL_TEXT = "…";

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
     * Returns the formatted text of the given tree-node.
     *
     * @param node Tree-node.
     * @return Formatted string.
     */
    @Override
    public String getText(Tree node) {

        if (node instanceof TerminalNode) {
            return getLabelForToken(((TerminalNode) node).getSymbol());
            //Trees.getNodeText(node, Arrays.asList(parser.getRuleNames()));
        }

        String text = "?";
        if (node instanceof PreviewInterpreterRuleContext) {
            Rule rule = getRule(node);
            String[] altLabels = getAltLabels(rule);
            int outerAltNum = getOuterAltNum(node);
            text = rule.name;

            if (altLabels != null) {
                if (outerAltNum >= 0 && (outerAltNum < altLabels.length)) {
                    if (compact) text = '#' + altLabels[outerAltNum];
                    else text += '[' + altLabels[outerAltNum] + ']';
                }
            }

            if (rule.getOriginalNumberOfAlts() > 1 && !compact) {
                text += ALT_LABEL_TEXT + outerAltNum;
            }
        }

        return text;
    }


    /**
     * Returns the formatted label of a given token.
     *
     * @param token Token.
     * @return Label as string.
     */
    private String getLabelForToken(Token token) {
        String text = token.getText();
        String symName = parser.getVocabulary().getSymbolicName(token.getType());

        // prevent node label getting to long
        if (text.length() > MAX_TOKEN_LENGTH)
            text = text.substring(0, MAX_TOKEN_LENGTH) + SHORTEN_LABEL_TEXT;

        if (text.equals("<EOF>")) return EOF_LABEL;
        if (symName == null) return text;

        if (compact) return text;

        return symName + ": " + text;
    }


    public boolean isCompact() {
        return compact;
    }


    public void setCompact(boolean compact) {
        this.compact = compact;
    }


    /**
     * Returns the associated rule of the given tree-node.
     *
     * @param node Tree-node.
     * @return The Rule.
     */
    private Rule getRule(Tree node) {
        PreviewInterpreterRuleContext inode = (PreviewInterpreterRuleContext) node;
        return g.getRule(inode.getRuleIndex());
    }


    /**
     * The predicted outermost alternative for the rule associated with this context object.
     * If left recursive, the true original outermost alternative is returned.
     *
     * @param node Tree-node to examine.
     * @return Alt num.
     */
    private int getOuterAltNum(Tree node) {
        return ((PreviewInterpreterRuleContext) node).getOuterAltNum();

    }


}
