package org.antlr.intellij.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.PsiTreeMatcher;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static org.antlr.intellij.plugin.psi.MyPsiUtils.isRuleElement;
import static org.antlr.intellij.plugin.psi.MyPsiUtils.isTokenElement;

/**
 * Annotator providing basic semantic highlighting via PsiTree structure matching.
 *
 * @see org.antlr.intellij.plugin.psi.PsiTreeMatcher
 */
public class ANTLR_v4SemanticHighlighter implements Annotator {
    /**
     * Static list containing all tree-matchers for validating.
     */
    private static final List<PsiTreeMatcher<PsiElement, TextAttributesKey>>
        matchers = new ArrayList<>();
    
    /*|--------------------------------------------------------------------------|*/
    
    public static final TextAttributesKey RULE_DECL =
        createTextAttributesKey("ANTLRv4_RULE_LABEL", DefaultLanguageHighlighterColors.CONSTANT);
    
    public static final TextAttributesKey RULE_LABEL =
        createTextAttributesKey("ANTLRv4_RULE_LABEL", DefaultLanguageHighlighterColors.CONSTANT);
    
    /*|--------------------------------------------------------------------------|*/
    
    
    static {
        var ruleDeclMatcher = new PsiTreeMatcher<PsiElement, TextAttributesKey>(RULE_DECL);
        ruleDeclMatcher.addPremise(
            element -> isTokenElement(element, ANTLRv4Lexer.RULE_REF),
            element -> element.getParent() != null,
            element -> isRuleElement(element.getParent(), ANTLRv4Parser.RULE_parserRuleSpec
            )
        );
        
        matchers.add(ruleDeclMatcher);
        
        var ruleLabelMatcher = new PsiTreeMatcher<PsiElement, TextAttributesKey>(RULE_LABEL);
        ruleLabelMatcher.addPremise(
            element -> isTokenElement(element, ANTLRv4Lexer.RULE_REF),
            element -> element.getParent() != null && element.getParent().getParent() != null,
            element -> isRuleElement(element.getParent().getParent(), ANTLRv4Parser.RULE_labeledAlt
            )
        );
        
        matchers.add(ruleLabelMatcher);
    }
    
    
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Helper method for adding an annotation.
     *
     * @param psiTreeMatcher The tree-macher.
     * @param element        The PsiElement.
     * @param holder         The holder.
     */
    private void addAnnotation(PsiTreeMatcher<PsiElement, TextAttributesKey> psiTreeMatcher, @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.getTextRange())
            .textAttributes(psiTreeMatcher.getAttribute())
            .create();
    }
    
    
    /**
     * Annotates the specified PSI element.
     * It is guaranteed to be executed in non-reentrant fashion.
     * I.e, there will be no call of this method for this instance before previous call get completed.
     * Multiple instances of the annotator might exist simultaneously, though.
     *
     * @param element to annotate.
     * @param holder  the container which receives annotations created by the plugin.
     */
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        for (var psiTreeMatcher : matchers) {
            if (psiTreeMatcher.matches(element)) {
                addAnnotation(psiTreeMatcher, element, holder);
            }
        }
    }
    
}
