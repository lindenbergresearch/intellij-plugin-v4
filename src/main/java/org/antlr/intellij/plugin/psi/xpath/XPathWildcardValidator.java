package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class XPathWildcardValidator extends XPathValidator {
    /**
     * Forbid direct instantiation, only singleton is allowed.
     */
    private XPathWildcardValidator() {}
   
    /**
     * Singleton instance.
     */
    static final XPathWildcardValidator instance =
        new XPathWildcardValidator();
    
    
    /**
     * Returns the singleton instance.
     *
     * @return Singleton.
     */
    public static XPathWildcardValidator getInstance() {
        return instance;
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    /**
     * Regular-expression for matching a parent element.
     */
    final static String regex = "^([a-zA-Z0-9_\\*]+)$";
    
    
    /**
     * Checks of the xpath-expression is well-formed.
     *
     * @param pathExpr The xpath-expression
     * @return True if well-formed.
     */
    @Override
    public boolean setPathExpr(String pathExpr) {
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
