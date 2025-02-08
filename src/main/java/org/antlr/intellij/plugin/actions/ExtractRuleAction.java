package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.refactoring.IntroduceTargetChooser;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

/**
 * Extracts an expression to an new parser rule.
 */
public class ExtractRuleAction extends AnAction {
    
    /**
     * Enables the action if the caret is in a lexer or parser rule.
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        var presentation = e.getPresentation();
        
        var grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
        if (grammarFile == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var parserRule = MyActionUtils.getParserRuleSurroundingRef(e);
        var lexerRule = MyActionUtils.getLexerRuleSurroundingRef(e);
        if (parserRule == null && lexerRule == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            var el = MyActionUtils.getSelectedPsiElement(e);
            if (el == null || findExtractableRules(el).isEmpty()) {
                presentation.setEnabled(false);
                return;
            }
        }
        
        // TODO: disable if selection spans rules
        presentation.setEnabled(true);
    }
    
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var el = MyActionUtils.getSelectedPsiElement(e);
        if (el == null) return;
        
        final var psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) return;
        
        var editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) return;
        var selectionModel = editor.getSelectionModel();
        
        if (!selectionModel.hasSelection()) {
            var expressions = findExtractableRules(el);
            
            IntroduceTargetChooser.showChooser(editor, expressions, new Pass<>() {
                @Override
                public void pass(PsiElement element) {
                    selectionModel.setSelection(element.getTextOffset(), element.getTextRange().getEndOffset());
                    extractSelection(psiFile, editor, selectionModel);
                }
            }, psiElement -> psiElement.getText());
        } else {
            extractSelection(psiFile, editor, selectionModel);
        }
    }
    
    
    @NotNull
    private List<PsiElement> findExtractableRules(PsiElement context) {
        List<PsiElement> expressions = new ArrayList<>();
        
        Set<IElementType> candidateTypes = Stream.of(ANTLRv4Parser.RULE_element, ANTLRv4Parser.RULE_alternative)
            .map(ANTLRv4TokenTypes::getRuleElementType)
            .collect(Collectors.toSet());
        
        @Nullable PsiElement parent = context;
        while (parent != null) {
            if (parent.getNode() != null && candidateTypes.contains(parent.getNode().getElementType())) {
                expressions.add(parent);
            }
            
            parent = parent.getParent();
        }
        return expressions;
    }
    
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    
    private void extractSelection(@NotNull PsiFile psiFile, Editor editor, SelectionModel selectionModel) {
        var doc = editor.getDocument();
        var grammarText = psiFile.getText();
        var results = ParsingUtils.parseANTLRGrammar(grammarText);
        final var parser = results.parser;
        final var tree = (ParserRuleContext) results.tree;
        var tokens = parser.getTokenStream();
        
        var selStart = selectionModel.getSelectionStart();
        var selStop = selectionModel.getSelectionEnd() - 1; // I'm inclusive and they are exclusive for end offset
        
        // find appropriate tokens for bounds, don't include WS
        var start = RefactorUtils.getTokenForCharIndex(tokens, selStart);
        var stop = RefactorUtils.getTokenForCharIndex(tokens, selStop);
        if (start == null || stop == null) {
            return;
        }
        if (start.getType() == ANTLRv4Lexer.WS) {
            start = tokens.get(start.getTokenIndex() + 1);
        }
        if (stop.getType() == ANTLRv4Lexer.WS) {
            stop = tokens.get(stop.getTokenIndex() - 1);
        }
        
        selectionModel.setSelection(start.getStartIndex(), stop.getStopIndex() + 1);
        final var project = psiFile.getProject();
        final var nameChooser = new ChooseExtractedRuleName(project);
        nameChooser.show();
        if (nameChooser.ruleName == null) return;
        
        // make new rule string
        final var ruleText = selectionModel.getSelectedText();
        
        final var insertionPoint = RefactorUtils.getCharIndexOfNextRuleStart(tree, start.getTokenIndex());
        final var newRule = '\n' + nameChooser.ruleName + " : " + ruleText + " ;" + '\n';
        
        runWriteCommandAction(project, () -> {
            // do all as one operation.
            doc.insertString(Math.min(insertionPoint, doc.getTextLength()), newRule);
            doc.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), nameChooser.ruleName);
        });
        
        // TODO: only allow selection of fully-formed syntactic entity.
        // E.g., "A (',' A" is invalid grammatically as a rule.
    }
}
