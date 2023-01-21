package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
/**
 * Plain validator for matching exact path names. final String regex = "^\\$([a-zA-Z_][a-zA-Z0-9_]*)$";
 */
public class XPathPlainValidator extends XPathValidator {
    /**
     * Forbid direct instantiation, only singleton is allowed.
     */
    private XPathPlainValidator() {}
    
    
    /**
     * Singleton instance.
     */
    static final XPathPlainValidator instance =
        new XPathPlainValidator();
    
    
    /**
     * Returns the singleton instance.
     *
     * @return Singleton.
     */
    public static XPathPlainValidator getInstance() {
        return instance;
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    /**
     * Regular-expression for matching plain, exact path names.
     */
    private final static String regex = "^([a-zA-Z_][a-zA-Z0-9_]*)$";
    
    
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
        
        for (var child : psiElement.getChildren()) {
            if (child.toString().equals(intPathExpr))
                elems.add(child);
        }
        
        return elems;
    }
}
