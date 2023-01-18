package org.antlr.intellij.plugin.psi.xpath;

import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.List;

public class XPathCountElement extends XPathElement {
    private final static String regex = "^#([0-9]+)$";
    
    
    public XPathCountElement(String path, String label) {
        super(path, label);
    }
    
    
    public static String getRegex() {
        return regex;
    }
    
    
    public XPathCountElement(String path) {
        super(path);
    }
    
    
    @Override
    public List<PsiElement> resolve(PsiElement psiElement) {
        int count;
        
        try {
            count = Integer.parseInt(pathExpr);
        } catch (NumberFormatException e) {
            return null;
        }
        
        if (psiElement.getChildren() != null && psiElement.getChildren().length == count)
            return Arrays.asList(psiElement.getChildren());
        
        return null;
    }
}
