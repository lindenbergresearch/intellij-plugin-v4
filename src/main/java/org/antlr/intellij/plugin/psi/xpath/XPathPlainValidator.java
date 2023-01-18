package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain validator for matching exact path names.
 */
public class XPathPlainValidator extends XPathValidator {
    /**
     * Regular-expression for matching plain, exact path names.
     */
    private final static String regex = "^([a-zA-Z_][a-zA-Z0-9_]*)$";
    
    
    /**
     * Creates a new validator instance.
     *
     * @param path  The path-expression.
     * @param label The label
     */
    public XPathPlainValidator(String path, String label) {
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
     * Returns 1 child who matches the given path-expression.
     *
     * @param psiElement Psi-element to resolve.
     * @return A list of PsiElements or empty List.
     */
    @Override
    public List<PsiElement> resolve(PsiElement psiElement) {
        var elems = new ArrayList<PsiElement>();
        
        for (var child : psiElement.getChildren()) {
            if (child.toString().equals(pathExpr))
                elems.add(child);
        }
        
        return elems;
    }
}
