package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import org.antlr.intellij.plugin.generators.LiteralChooser;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.xpath.XPath;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class GenerateLexerRulesForLiteralsAction extends AnAction {
    public static final Logger LOG = Logger.getInstance("GenerateLexerRulesForLiterals");
    
    
    /**
     * Only show if selection is a literal
     */
    @Override
    public void update(AnActionEvent e) {
        var presentation = e.getPresentation();
        var grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
        
        if (grammarFile == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var file = e.getData(LangDataKeys.PSI_FILE);
        var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) throw new AssertionError();
        if (file == null) throw new AssertionError();
        var selectedElement = BaseRefactoringAction.getElementAtCaret(editor, file);
        if (selectedElement == null) { // we clicked somewhere outside text
            presentation.setEnabled(false);
        }
    }
    
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    
    @Override
    public void actionPerformed(AnActionEvent e) {
        LOG.info("actionPerformed GenerateLexerRulesForLiteralsAction");
        final var project = e.getProject();
        
        final var psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }
        
        var inputText = psiFile.getText();
        var results = ParsingUtils.parseANTLRGrammar(inputText);
        
        final var parser = results.parser;
        final var tree = results.tree;
        var literalNodes = XPath.findAll(tree, "//ruleBlock//STRING_LITERAL", parser);
        var lexerRules = new LinkedHashMap<String, String>();
       
        for (var node : literalNodes) {
            var literal = node.getText();
            var ruleText = String.format("%s : %s ;",
                RefactorUtils.getLexerRuleNameFromLiteral(literal), literal);
            lexerRules.put(literal, ruleText);
        }
        
        // remove those already defined
        var lexerRulesXPath = "//lexerRule";
        var treePattern = "<TOKEN_REF> : <STRING_LITERAL>;";
        var p = parser.compileParseTreePattern(treePattern, ANTLRv4Parser.RULE_lexerRule);
        var matches = p.findAll(tree, lexerRulesXPath);
        
        for (var match : matches) {
            var lit = match.get("STRING_LITERAL");
            // we have rule for this literal already
            lexerRules.remove(lit.getText());
        }
        
        final var chooser =
            new LiteralChooser(project, new ArrayList<>(lexerRules.values()));
        chooser.show();
        var selectedElements = chooser.getSelectedElements();
        // chooser disposed automatically.
        
        final var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) throw new AssertionError();
        final var doc = editor.getDocument();
        final var tokens = (CommonTokenStream) parser.getTokenStream();
       
        if (selectedElements != null) {
            var text = doc.getText();
            var cursorOffset = editor.getCaretModel().getOffset();
            // make sure it's not in middle of rule; put between.
            var allRuleNodes = XPath.findAll(tree, "//ruleSpec", parser);
            for (var r : allRuleNodes) {
                var extent = r.getSourceInterval(); // token indexes
                var start = tokens.get(extent.a).getStartIndex();
                var stop = tokens.get(extent.b).getStopIndex();
                if (cursorOffset < start) {
                    // before this rule, so must be between previous and this one
                    cursorOffset = start; // put right before this rule
                    break;
                }
                if (cursorOffset >= start && cursorOffset <= stop) {
                    // cursor in this rule
                    cursorOffset = stop + 2; // put right before this rule (after newline)
                    if (cursorOffset >= text.length()) {
                        cursorOffset = text.length();
                    }
                    break;
                }
            }
            
            var allRules = Utils.join(selectedElements.iterator(), "\n");
            text =
                text.substring(0, cursorOffset) +
                    '\n' + allRules + '\n' +
                    text.substring(cursorOffset);
            MyPsiUtils.replacePsiFileFromText(project, psiFile, text);
        }
    }
}
