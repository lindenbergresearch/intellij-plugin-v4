package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Abstract XPathElement.
 * Represents a path element of the path-expression in
 * its validator form.
 */
public abstract class XPathValidator implements XPathExprMatcher {
    
    /**
     * Resolver method, to be overridden in subclass to implement
     * the specific matcher for this path-expression.
     *
     * @param psiElement Psi-element to resolve.
     * @return A list of resolvable psi-elements.
     */
    abstract public List<PsiElement> resolve(PsiElement psiElement);
    
    
    /**
     * Checks of the xpath-expression is well-formed.
     *
     * @param pathExpr The xpath-expression
     * @return True if well-formed.
     */
    abstract boolean setPathExpr(String pathExpr);
    
    /*|--------------------------------------------------------------------------|*/
    
    /**
     * Internal representation of the path-expression.
     */
    String intPathExpr = "";
    
    
    /**
     * The Regex pattern and matcher.
     */
    Pattern pattern = null;
    Matcher matcher = null;
    
    
    /**
     * A list of all available validators.
     */
    static List<XPathValidator> validators;
    
    
    static {
        validators = new ArrayList<>();
        validators.add(XPathPlainValidator.getInstance());
        validators.add(XPathCountValidator.getInstance());
        validators.add(XPathParentValidator.getInstance());
        validators.add(XPathWildcardValidator.getInstance());
    }
    
    
    /**
     * Setup and compile pattern.
     *
     * @param regex    The regex to validate the path.
     * @param pathExpr The path-expression.
     */
    protected void setPattern(String regex, String pathExpr) throws PatternSyntaxException {
        if (regex == null)
            return;
        
        pattern = Pattern.compile(regex);
        
        if (pathExpr != null)
            matcher = pattern.matcher(pathExpr);
    }
    
    
    /**
     * Build an XPathElement out of a given path-expression.
     *
     * @param pathExpr The path-expression.
     * @return A new instance if it could be matched or null otherwise.
     */
    public static XPathValidator fromString(String pathExpr) {
        for (var validator : validators) {
            if (validator.setPathExpr(pathExpr))
                return validator;
        }
        
        return null;
    }
    
}
