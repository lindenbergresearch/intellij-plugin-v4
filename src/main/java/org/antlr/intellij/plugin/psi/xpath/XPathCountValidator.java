package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * XPath count validator.
 */
public class XPathCountValidator extends XPathValidator {
    /**
     * Forbid direct instantiation, only singleton is allowed.
     */
    private XPathCountValidator() {}
   
    /**
     * Singleton instance.
     */
    static final XPathCountValidator instance =
        new XPathCountValidator();
    
    
    /**
     * Returns the singleton instance.
     *
     * @return Singelton.
     */
    public static XPathCountValidator getInstance() {
        return instance;
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    /**
     * Matching regex for numbers only.
     */
    private final static String regex = "^#([0-9]+)$";
    
    
    /**
     * Checks of the xpath-expression is well-formed.
     *
     * @param pathExpr The xpath-expression
     * @return True if well-formed.
     */
    @Override
    public boolean setPathExpr(String pathExpr) {
        try {
            setPattern(regex, pathExpr);
        } catch (PatternSyntaxException e) {
            return false;
        }
        
        if (matcher.find() && matcher.groupCount() == 1) {
            intPathExpr = matcher.group(0);
            return true;
        }
        
        return false;
    }
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Resolver method, to be overridden in subclass to implement
     * the specific matcher for this path-expression.
     *
     * @param psiElement Psi-element to resolve.
     * @param pathExpr   The path-expression to used for resolving.
     * @return A list of resolvable psi-elements.
     */
    @Override
    public List<PsiElement> resolve(PsiElement psiElement) {
        int count;
        
        try {
            count = Integer.parseInt(intPathExpr);
        } catch (NumberFormatException e) {
            return null;
        }
        
        if (psiElement.getChildren().length == count)
            return Arrays.asList(psiElement.getChildren());
        
        return null;
    }
}
