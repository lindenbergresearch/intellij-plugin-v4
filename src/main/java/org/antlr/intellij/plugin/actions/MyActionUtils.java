package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.intellij.plugin.psi.*;
import org.antlr.v4.runtime.atn.DecisionEventInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MyActionUtils {
    public static void selectedFileIsGrammar(AnActionEvent e) {
        var virtualFile = getGrammarFileFromEvent(e);
        
        if (virtualFile == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        e.getPresentation().setEnabled(true); // enable action if we're looking at grammar file
        e.getPresentation().setVisible(true);
    }
    
    
    public static VirtualFile getGrammarFileFromEvent(AnActionEvent e) {
        var files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        if (files == null || files.length == 0) {
            return null;
        }
        
        var virtualFile = files[0];
        if (virtualFile != null && virtualFile.getName().endsWith(".g4")) {
            return virtualFile;
        }
        
        return null;
    }
    
    
    public static int getMouseOffset(MouseEvent mouseEvent, Editor editor) {
        var point = new Point(mouseEvent.getPoint());
        var pos = editor.xyToLogicalPosition(point);
        return editor.logicalPositionToOffset(pos);
    }
    
    
    public static int getMouseOffset(Editor editor) {
        var mousePosition = editor.getContentComponent().getMousePosition();
        var pos = editor.xyToLogicalPosition(mousePosition);
        return editor.logicalPositionToOffset(pos);
    }
    
    
    public static void moveCursor(Editor editor, int cursorOffset) {
        var caretModel = editor.getCaretModel();
        caretModel.moveToOffset(cursorOffset);
        var scrollingModel = editor.getScrollingModel();
        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
        editor.getContentComponent().requestFocus();
    }
    
    
    @NotNull
    public static List<RangeHighlighter> getRangeHighlightersAtOffset(Editor editor, int offset) {
        var markupModel = editor.getMarkupModel();
        // collect all highlighters and combine to make a single tool tip
        List<RangeHighlighter> highlightersAtOffset = new ArrayList<>();
        for (var r : markupModel.getAllHighlighters()) {
            var a = r.getStartOffset();
            var b = r.getEndOffset();
            
            if (offset >= a && offset < b) { // cursor is over some kind of highlighting
                highlightersAtOffset.add(r);
            }
        }
        
        return highlightersAtOffset;
    }
    
    
    public static DecisionEventInfo getHighlighterWithDecisionEventType(List<RangeHighlighter> highlighters, Class<?> decisionEventType) {
        for (var r : highlighters) {
            var eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
            
            if (eventInfo != null) {
                if (eventInfo.getClass() == decisionEventType) {
                    return eventInfo;
                }
            }
        }
        
        return null;
    }
    
    
    public static ParserRuleRefNode getParserRuleSurroundingRef(AnActionEvent e) {
        var selectedPsiNode = getSelectedPsiElement(e);
        var ruleSpecNode = getRuleSurroundingRef(selectedPsiNode, ParserRuleSpecNode.class);
        
        if (ruleSpecNode == null) {
            return null;
        }
        
        // find the name of rule under ParserRuleSpecNode
        return PsiTreeUtil.findChildOfType(ruleSpecNode, ParserRuleRefNode.class);
    }
    
    
    public static ParserRuleRefNode getParserRuleSurroundingRef(PsiElement element) {
        var ruleSpecNode = getRuleSurroundingRef(element, ParserRuleSpecNode.class);
        if (ruleSpecNode == null) {
            return null;
        }
        
        // find the name of rule under ParserRuleSpecNode
        return PsiTreeUtil.findChildOfType(ruleSpecNode, ParserRuleRefNode.class);
    }
    
    
    public static LexerRuleRefNode getLexerRuleSurroundingRef(AnActionEvent e) {
        var selectedPsiNode = getSelectedPsiElement(e);
        var ruleSpecNode = getRuleSurroundingRef(selectedPsiNode, LexerRuleSpecNode.class);
        if (ruleSpecNode == null) {
            return null;
        }
        
        // find the name of rule under ParserRuleSpecNode
        return PsiTreeUtil.findChildOfType(ruleSpecNode, LexerRuleRefNode.class);
    }
    
    
    public static RuleSpecNode getRuleSurroundingRef(PsiElement selectedPsiNode, final Class<? extends RuleSpecNode> ruleSpecNodeClass) {
        if (selectedPsiNode == null) { // didn't select a node in parse tree
            return null;
        }
        
        // find root of rule def
        if (selectedPsiNode.getClass() != ruleSpecNodeClass) {
            selectedPsiNode = PsiTreeUtil.findFirstParent(
                selectedPsiNode, psiElement -> psiElement.getClass() == ruleSpecNodeClass
            );
            
            if (selectedPsiNode == null) { // not in rule I guess.
                return null;
            }
            // found rule
        }
        
        return (RuleSpecNode) selectedPsiNode;
    }
    
    
    public static PsiElement getSelectedPsiElement(AnActionEvent e) {
        var editor = e.getData(PlatformDataKeys.EDITOR);
        
        if (editor == null) { // not in editor
            var selectedNavElement = e.getData(LangDataKeys.PSI_ELEMENT);
            // in nav bar?
            if (!(selectedNavElement instanceof ParserRuleRefNode)) {
                return null;
            }
            
            return selectedNavElement;
        }
        
        // in editor
        var file = e.getData(LangDataKeys.PSI_FILE);
        if (file == null) {
            return null;
        }
        
        var offset = editor.getCaretModel().getOffset();
        return file.findElementAt(offset);
    }
    
    
    /**
     * Only show if selection is a lexer or parser rule
     */
    public static void showOnlyIfSelectionIsRule(AnActionEvent e, String title) {
        var presentation = e.getPresentation();
        var grammarFile = getGrammarFileFromEvent(e);
        if (grammarFile == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var el = getSelectedPsiElement(e);
        if (el == null) {
            presentation.setEnabled(false);
            return;
        }
        
        var parserRule = getParserRuleSurroundingRef(e);
        var lexerRule = getLexerRuleSurroundingRef(e);
        
        if ((lexerRule != null && el instanceof LexerRuleRefNode) ||
            (parserRule != null && el instanceof ParserRuleRefNode)) {
            var ruleName = el.getText();
            presentation.setText(String.format(title, ruleName));
        } else {
            presentation.setEnabled(false);
        }
    }
}
