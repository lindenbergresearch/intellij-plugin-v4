package org.antlr.intellij.plugin.editor;


import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.jetbrains.annotations.Nullable;

public class ANTLRDocumentationProvider implements DocumentationProvider {
    
    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element == null) return null;
        
        if (element instanceof LexerRuleSpecNode) {
            return '(' + element.getText() + ")(" + originalElement.getText() + ", class=" + element.getClass().getSimpleName() + ')';
        }
        
        return null;
    }
}
