package org.antlr.intellij.plugin.actions;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications.Bus;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiDocumentManager;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.v4.runtime.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Generate parser from ANTLR grammar;
 * learned how to do from Grammar-Kit by Gregory Shrago.
 */
public class GenerateParserAction extends AnAction implements DumbAware {
    public static final Logger LOG = Logger.getInstance(GenerateParserAction.class);
    
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        MyActionUtils.selectedFileIsGrammar(e);
    }
    
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    
    @Override
    public void actionPerformed(final AnActionEvent e) {
        var project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            LOG.error("actionPerformed no project for " + e);
            return; // whoa!
        }
        
        var grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
        LOG.info("actionPerformed " + (grammarFile == null ? "NONE" : grammarFile));
        if (grammarFile == null) return;
        
        // commit changes to PSI and file system
        var psiMgr = PsiDocumentManager.getInstance(project);
        var docMgr = FileDocumentManager.getInstance();
        var doc = docMgr.getDocument(grammarFile);
        if (doc == null) return;
        
        var unsaved = !psiMgr.isCommitted(doc) || docMgr.isDocumentUnsaved(doc);
        if (unsaved) {
            // save event triggers ANTLR run if autogen on
            psiMgr.commitDocument(doc);
            docMgr.saveDocument(doc);
        }
        
        var forceGeneration = true; // from action, they really mean it
        var canBeCancelled = true;
        var title = "ANTLR Parser Generator";
        
        var gen = new RunANTLROnGrammarFile(grammarFile,
            project,
            title,
            canBeCancelled,
            forceGeneration
        );
        
        var autogen = ANTLRv4GrammarPropertiesStore.getGrammarProperties(project, grammarFile).shouldAutoGenerateParser();
        
        if (!unsaved || !autogen) {
            // if everything already saved (not stale) then run ANTLR
            // if had to be saved and autogen NOT on, then run ANTLR
            // Otherwise, the save file event will have or will run ANTLR.
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> gen.run(),
                title,
                canBeCancelled,
                project
            );
            
            // refresh from disk to see new files
            Set<File> generatedFiles = new HashSet<>();
            generatedFiles.add(new File(gen.getOutputDirName()));
            LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
            var files = Utils.join(generatedFiles.iterator(), ", ");
            
            ANTLRv4PluginController.printToConsole(project, "Generated grammar files: " + files, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            
            var msg = "Generated " +
                generatedFiles.size() + (generatedFiles.size() > 1 ? "files" : "file") +
                " for grammar: " +
                grammarFile.getName() +
                " written to: " +
                gen.getOutputDirName();
            
            // pop up a notification
            var notification =
                new Notification(RunANTLROnGrammarFile.groupDisplayId,
                    title, msg, NotificationType.INFORMATION
                );
            
            Bus.notify(notification, project);
        }
    }
}
