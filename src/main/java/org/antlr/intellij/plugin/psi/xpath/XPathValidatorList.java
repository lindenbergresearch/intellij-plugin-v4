package org.antlr.intellij.plugin.psi.xpath;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;


/**
 *
 */
public class XPathValidatorList extends ArrayList<XPathValidator> {
    
    public XPathValidatorList(int initialCapacity) {
        super(initialCapacity);
    }
    
    
    public XPathValidatorList() {
    }
    
    
    public XPathValidatorList(@NotNull Collection<? extends XPathValidator> c) {
        super(c);
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public XPathValidator[] asArray() {
        return this.toArray(new XPathValidator[0]);
    }
    
    
    public XPathValidator head() {
        return get(0);
    }
    
    
    public XPathValidatorList tail() {
        return new XPathValidatorList(subList(1, size()));
    }
    
}
