package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract XPathElement.
 * Represents a path element of the path expression in
 * its validator form.
 */
public abstract class XPathValidator {
    String pathExpr;
    String label;
    
    
    /**
     * Create a XPathElement out of a pathExpr-expression and a label.
     *
     * @param pathExpr  Path-expression.
     * @param label Label.
     */
    public XPathValidator(String pathExpr, String label) {
        this.pathExpr = pathExpr;
        this.label = label;
    }
    
    
    /**
     * Create a XPathElement out of a pathExpr-expression.
     *
     * @param pathExpr Path-expression.
     */
    public XPathValidator(String pathExpr) {
        this.pathExpr = pathExpr;
    }
    
    
    /**
     * Resolver method, to be overridden in subclass to implement
     * the specific matcher for this path-expression.
     *
     * @param psiElement Psi-element to resolve.
     * @return A list of resolvable psi-elements.
     */
    abstract public List<PsiElement> resolve(PsiElement psiElement);
    
    
    /**
     * Test a pathExpr string againsed a regex expression.
     *
     * @param pathExpr  The pathExpr-expression.
     * @param regex The regex.
     * @return True if matches a group.
     */
    public static boolean matches(String pathExpr, String regex) {
        final var pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final var matcher = pattern.matcher(pathExpr);
        
        return matcher.find() && matcher.groupCount() == 1;
        
    }
    
    
    /**
     * Extract the needed group from a pathExpr-expression via regex.
     *
     * @param pathExpr  The pathExpr-expression.
     * @param regex The regex.
     * @return The matched group or null if nothing could be matched.
     */
    public static String extract(String pathExpr, String regex) {
        final var pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final var matcher = pattern.matcher(pathExpr);
        
        if (matcher.find()) {
            if (matcher.groupCount() == 1) return matcher.group(0);
        }
        
        return null;
    }
    
    
    /**
     * Build an XPathElement out of a given path-expression.
     *
     * @param pathExpr The path-expression.
     * @return A new instance if it could be matched or null otherwise.
     */
    public static XPathValidator fromString(String pathExpr) {
        
        if (matches(pathExpr, XPathPlainValidator.getRegex()))
            return new XPathPlainValidator(extract(pathExpr, XPathPlainValidator.getRegex()), "Plain path matcher");
        
        if (matches(pathExpr, XPathCountValidator.getRegex())) {
            return new XPathCountValidator(extract(pathExpr, XPathCountValidator.getRegex()), "Count path matcher");
        }
        
        return null;
    }
    
}
