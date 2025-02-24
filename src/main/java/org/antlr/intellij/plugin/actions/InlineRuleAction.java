package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

public class InlineRuleAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        MyActionUtils.showOnlyIfSelectionIsRule(e, "Inline and Remove Rule %s");
    }
    
    
    @Override
    public void actionPerformed(AnActionEvent e) {
        var psiElement = MyActionUtils.getSelectedPsiElement(e);
        if (psiElement == null) {
            return;
        }
        
        final var ruleName = psiElement.getText();
        
        final var psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }
        
        final var project = e.getProject();
        
        var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        
        final var doc = editor.getDocument();
        
        var grammarText = psiFile.getText();
        var results = ParsingUtils.parseANTLRGrammar(grammarText);
        var parser = results.parser;
        var tree = results.tree;
        
        final var tokens = (CommonTokenStream) parser.getTokenStream();
        
        // find all parser and lexer rule refs
        final var refNodes = RefactorUtils.getAllRuleRefNodes(parser, tree, ruleName);
        if (refNodes == null) {
            return;
        }
        
        // find rule def
        ParseTree ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
        if (ruleDefNameNode == null) {
            return;
        }
        
        // identify rhs of rule
        final var ruleDefNode = (ParserRuleContext) ruleDefNameNode.getParent();
        var altRuleText = RefactorUtils.getRuleText(tokens, ruleDefNode);
        
        // if rule has outermost alt, must add (...) around insertion
        // Look for ruleBlock, lexerRuleBlock
        if (RefactorUtils.ruleHasMultipleOutermostAlts(parser, ruleDefNode)) {
            altRuleText = '(' + altRuleText + ')';
        }
        final var ruleText = altRuleText; // we ref from inner class; requires final
        
        WriteCommandAction.runWriteCommandAction(project, () -> {
            replaceRuleRefs(doc, tokens, ruleName, refNodes, ruleText);
        });
    }
    
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    
    public void replaceRuleRefs(
        Document doc, CommonTokenStream tokens,
        String ruleName,
        List<TerminalNode> refNodes,
        String ruleText
    ) {
        var base = 0;
        for (var t : refNodes) { // walk nodes in lexicographic order, replacing as we go
            var rrefToken = t.getSymbol();
            var nextToken = tokens.get(rrefToken.getTokenIndex() + 1);
            var thisReplacementRuleText = ruleText;
            if ((IntStream.of(
                ANTLRv4Lexer.STAR,
                ANTLRv4Lexer.PLUS,
                ANTLRv4Lexer.QUESTION
            ).anyMatch(i -> nextToken.getType() == i)) &&
                !ruleText.startsWith("(")) {
                // need (...) if we replace foo* or foo+ and ruleText doesn't have parens yet
                thisReplacementRuleText = '(' + ruleText + ')';
            }
            
            doc.replaceString(base + rrefToken.getStartIndex(), base + rrefToken.getStopIndex() + 1, thisReplacementRuleText);
            // text shifts underneath us so we adjust token start/stop indexes into doc
            base += thisReplacementRuleText.length() - ruleName.length();
        }
        
        // reparse to find new rule location
        var grammarText = doc.getText();
        var results = ParsingUtils.parseANTLRGrammar(grammarText);
        var parser = results.parser;
        var tree = results.tree;
        tokens = (CommonTokenStream) parser.getTokenStream();
        
        // find rule def
        var ruleDefNameNode = RefactorUtils.getRuleDefNameNode(parser, tree, ruleName);
        if (ruleDefNameNode == null) {
            return;
        }
        
        final var ruleDefNode = (ParserRuleContext) ruleDefNameNode.getParent();
        var start = ruleDefNode.getStart();
        var stop = ruleDefNode.getStop();
        
        // check for direct recursive, in which case we don't delete it
        var ruleIsDirectlyRecursive = false;
        for (var t : refNodes) {
            if (Trees.isAncestorOf(ruleDefNode, t)) {
                ruleIsDirectlyRecursive = true;
            }
        }
        
        // don't delete if we made replacements in the rule itself
        if (ruleIsDirectlyRecursive) {
            return;
        }
        
        // remove the inlined rule (lexer or parser)
        var hiddenTokensToRight = tokens.getHiddenTokensToRight(stop.getTokenIndex());
        if (hiddenTokensToRight != null && !hiddenTokensToRight.isEmpty()) {
            // remove extra whitespace but not trailing comments (if any)
            // javadoc is included in start (if any) as it's not hidden
            var afterSemi = hiddenTokensToRight.get(0);
            if (afterSemi.getType() == ANTLRv4Lexer.WS) {
                stop = afterSemi;
            }
        }
        
        doc.deleteString(start.getStartIndex(), stop.getStopIndex() + 1);
    }
}
