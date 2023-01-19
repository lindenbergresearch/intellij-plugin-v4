package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class XPathParentValidator extends XPathValidator {
    /**
     * Forbid direct instantiation, only singleton is allowed.
     */
    private XPathParentValidator() {}
    
    /**
     * Singleton instance.
     */
    static final XPathParentValidator instance =
        new XPathParentValidator();
    
    
    /**
     * Returns the singleton instance.
     *
     * @return Singleton.
     */
    public static XPathParentValidator getInstance() {
        return instance;
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    /**
     * Regular-expression for matching a parent element.
     */
    private final static String regex = "^\\.\\.$";
    
    
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
        
        if (matcher.matches()) {
            intPathExpr = pathExpr;
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
     * @return A list of resolvable psi-elements.
     */
    @Override
    public List<PsiElement> resolve(PsiElement psiElement) {
        var elems = new ArrayList<PsiElement>();
        
        if (psiElement.getParent() != null)
            elems.add(psiElement.getParent());
        
        return elems;
    }
    
}
