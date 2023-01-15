package org.antlr.intellij.plugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;

import java.awt.event.MouseEvent;

public class TestRuleAction extends AnAction implements DumbAware {
    public static final Logger LOG = Logger.getInstance("ANTLR TestRuleAction");
    
    
    /**
     * Only show if selection is a grammar and in a rule
     */
    @Override
    public void update(AnActionEvent e) {
        var presentation = e.getPresentation();
        presentation.setText("Test ANTLR Rule"); // default text
        presentation.setIcon(AllIcons.Actions.Execute);
        
        var grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
        if (grammarFile == null) { // we clicked somewhere outside text or non grammar file
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        
        ParserRuleRefNode r = null;
        var inputEvent = e.getInputEvent();
        if (inputEvent instanceof MouseEvent) { // this seems to be after update() called 2x and we have selected the action
            r = MyActionUtils.getParserRuleSurroundingRef(e);
        } else {
            // If editor component, mouse event not happened yet to update caret so must ask for mouse position
            var editor = e.getData(PlatformDataKeys.EDITOR);
            if (editor != null) {
                var mousePosition = editor.getContentComponent().getMousePosition();
                if (mousePosition != null) {
                    var pos = editor.xyToLogicalPosition(mousePosition);
                    var offset = editor.logicalPositionToOffset(pos);
                    var file = e.getData(LangDataKeys.PSI_FILE);
                    if (file != null) {
                        var el = file.findElementAt(offset);
                        if (el != null) {
                            r = MyActionUtils.getParserRuleSurroundingRef(el);
                        }
                    }
                }
            }
            if (r == null) {
                r = MyActionUtils.getParserRuleSurroundingRef(e);
            }
        }
        if (r == null) {
            presentation.setEnabled(false);
            return;
        }
        
        presentation.setVisible(true);
        var ruleName = r.getText();
        if (Character.isLowerCase(ruleName.charAt(0))) {
            presentation.setEnabled(true);
            presentation.setText("Test Rule " + ruleName);
        } else {
            presentation.setEnabled(false);
        }
    }
    
    
    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (e.getProject() == null) {
            LOG.error("actionPerformed no project for " + e);
            return; // whoa!
        }
        var grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
        if (grammarFile == null) return;
        
        LOG.info("actionPerformed " + grammarFile);
        
        var controller = ANTLRv4PluginController.getInstance(e.getProject());
        controller.getPreviewWindow().show(null);
        
        var r = MyActionUtils.getParserRuleSurroundingRef(e);
        if (r == null) {
            return; // weird. no rule name.
        }
        var ruleName = r.getText();
        var docMgr = FileDocumentManager.getInstance();
        var doc = docMgr.getDocument(grammarFile);
        if (doc != null) {
            docMgr.saveDocument(doc);
        }
        
        controller.setStartRuleNameEvent(grammarFile, ruleName);
    }
    
}
