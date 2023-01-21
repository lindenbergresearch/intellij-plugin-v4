package org.antlr.intellij.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.psi.xpath.PsiXPathSelector;
import org.jetbrains.annotations.NotNull;

public class SemanticHighlighterAnnotation implements Annotator {
    
    
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
        
        var validator = new PsiXPathSelector(element);
        
        if (validator.validate("PsiWhiteSpace")) {
            System.out.println("\nVALIDATED!\n");
            System.out.println("element: '" + element + "' node: '" + element.getNode().getElementType() + "' child: #" + element.getChildren().length + " parent: '" + element.getParent() + "'.");
        }

//        if (isRuleElement(element, ANTLRv4Parser.RULE_identifier)) {
//            System.out.println("text: " + ((ASTWrapperPsiElement) element).getText());
//            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element.getTextRange()).textAttributes(DefaultLanguageHighlighterColors.CLASS_REFERENCE).create();
//        }

//        if (!(element instanceof ParserRuleRefNode)) {
//            return;
//        }
//
//
//        var node = (ParserRuleRefNode) element;
//
//
//        var text = node.getText();
//        var name = node.getName();
//
//        var start = node.getTextRange().getStartOffset();
//        var end = node.getTextRange().getEndOffset();
//
//        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
//            .range(node.getTextRange()).textAttributes(DefaultLanguageHighlighterColors.KEYWORD).create();
//
        // LOG.info(" name: " + name + " text: " + text.substring(start, end));
        
    }
}
