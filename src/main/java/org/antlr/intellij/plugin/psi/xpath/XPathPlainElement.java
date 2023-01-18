package org.antlr.intellij.plugin.psi.xpath;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class XPathPlainElement extends XPathElement {
    private final static String regex = "^\\[(.+)\\]$";;
    
    
    public XPathPlainElement(String path, String label) {
        super(path, label);
    }
    
    
    public XPathPlainElement(String path) {
        super(path);
    }
    
    
    public static String getRegex() {
        return regex;
    }
    
    
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
