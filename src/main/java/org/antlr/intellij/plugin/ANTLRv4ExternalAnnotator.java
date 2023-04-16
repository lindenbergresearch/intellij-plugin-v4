package org.antlr.intellij.plugin;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.actions.AnnotationIntentActionsFactory;
import org.antlr.intellij.plugin.validation.GrammarIssue;
import org.antlr.intellij.plugin.validation.GrammarIssuesCollector;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ANTLRv4ExternalAnnotator extends ExternalAnnotator<PsiFile, List<GrammarIssue>> {
    public static final Logger LOG = Logger.getInstance("ANTLRv4ExternalAnnotator");
    
    
    static void registerFixForAnnotation(Annotation annotation, GrammarIssue issue, PsiFile file) {
        var range = new TextRange(annotation.getStartOffset(), annotation.getEndOffset());
        var intentionAction = AnnotationIntentActionsFactory.getFix(range, issue.getMsg().getErrorType(), file);
        
        //TODO: replace deprecated methods
        intentionAction.ifPresent(fix -> annotation.registerFix(fix));
    }
    
    
    /**
     * Called first; return file
     */
    @Override
    @Nullable
    public PsiFile collectInformation(@NotNull PsiFile file) {
        return file;
    }
    
    
    /**
     * Called 2nd; run antlr on file
     */
    @Nullable
    @Override
    public List<GrammarIssue> doAnnotate(final PsiFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<GrammarIssue>>) () ->
            GrammarIssuesCollector.collectGrammarIssues(file)
        );
    }
    
    
    /**
     * Called 3rd
     */
    @Override
    public void apply(@NotNull PsiFile file, List<GrammarIssue> issues, @NotNull AnnotationHolder holder) {
        for (var issue : issues) {
            if (issue.getOffendingTokens().isEmpty()) {
                annotateFileIssue(file, holder, issue);
            } else {
                annotateIssue(file, holder, issue);
            }
        }
    }
    
    
    private void annotateFileIssue(@NotNull PsiFile file, @NotNull AnnotationHolder holder, GrammarIssue issue) {
        holder.newAnnotation(HighlightSeverity.WARNING, issue.getAnnotation()).fileLevel().create();
    }
    
    
    private void annotateIssue(@NotNull PsiFile file, @NotNull AnnotationHolder holder, GrammarIssue issue) {
        for (var t : issue.getOffendingTokens()) {
            if (t instanceof CommonToken && tokenBelongsToFile(t, file)) {
                var range = getTokenRange((CommonToken) t, file);
                var severity = getIssueSeverity(issue);
                var fix = AnnotationIntentActionsFactory.getFix(range, issue.getMsg().getErrorType(), file);
                
                holder.newAnnotation(severity, issue.getAnnotation()).range(range).create();
                fix.ifPresent(intentionAction ->
                    holder.newAnnotation(severity, issue.getAnnotation()).
                        newFix(intentionAction).
                        registerFix().
                        create()
                );
            }
        }
    }
    
    
    private HighlightSeverity getIssueSeverity(GrammarIssue issue) {
        var errorType = issue.getMsg().getErrorType();
        
        if (errorType == null)
            return HighlightSeverity.WEAK_WARNING;
        
        switch (errorType.severity) {
            case ERROR:
            case ERROR_ONE_OFF:
            case FATAL:
                return HighlightSeverity.ERROR;
            case WARNING:
            case WARNING_ONE_OFF:
                return HighlightSeverity.WARNING;
            default:
                return HighlightSeverity.WEAK_WARNING;
        }
    }
    
    
    @NotNull
    private TextRange getTokenRange(CommonToken ct, @NotNull PsiFile file) {
        var startIndex = ct.getStartIndex();
        var stopIndex = ct.getStopIndex();
        
        if (startIndex >= file.getTextLength()) {
            // can happen in case of a 'mismatched input EOF' error
            startIndex = stopIndex = file.getTextLength() - 1;
        }
        
        if (startIndex < 0) {
            // can happen on empty files, in that case we won't be able to show any error :/
            startIndex = 0;
        }
        
        return new TextRange(startIndex, stopIndex + 1);
    }
    
    
    private boolean tokenBelongsToFile(Token t, @NotNull PsiFile file) {
        var inputStream = t.getInputStream();
        // Not equal if the token belongs to an imported grammar
        return !(inputStream instanceof ANTLRFileStream) || inputStream.getSourceName().equals(file.getVirtualFile().getCanonicalPath());
    }
}
