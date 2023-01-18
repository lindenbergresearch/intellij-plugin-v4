package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.List;

/**
 * XPath count validator.
 */
public class XPathCountValidator extends XPathValidator {
    /**
     * Matching regex for numbers only.
     */
    private final static String regex = "^#([0-9]+)$";
    
    
    /**
     * Creates a new validator instance.
     *
     * @param path  The path-expression.
     * @param label The label
     */
    public XPathCountValidator(String path, String label) {
        super(path, label);
    }
    
    
    /**
     * Return the specific regex for this validator.
     *
     * @return Regex as string.
     */
    public static String getRegex() {
        return regex;
    }
    
    
    /**
     * Returns all children if the given number matches the total numbers of children.
     *
     * @param psiElement Psi-element to resolve.
     * @return A list of PsiElements or empty List.
     */
    @Override
    public List<PsiElement> resolve(PsiElement psiElement) {
        int count;
        
        try {
            count = Integer.parseInt(pathExpr);
        } catch (NumberFormatException e) {
            return null;
        }
        
        if (psiElement.getChildren().length == count)
            return Arrays.asList(psiElement.getChildren());
        
        return null;
    }
}
