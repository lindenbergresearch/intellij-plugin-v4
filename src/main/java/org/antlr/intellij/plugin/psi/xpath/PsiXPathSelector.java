package org.antlr.intellij.plugin.psi.xpath;


import com.intellij.psi.PsiElement;


/**
 * XPath validator for PsiElement trees.
 */
public class PsiXPathSelector {
    private PsiElement root;
    private String[] paths;
    
    private XPathValidatorList elements;
    private int index = 0;
    
    
    /**
     * Construct a new PsiXPathSelector with a given root node.
     *
     * @param node Root node.
     */
    public PsiXPathSelector(PsiElement node) {
        this.root = node;
    }


//    /**
//     * Test if a next path-element is available.
//     *
//     * @return True if available.
//     */
//    public boolean hasNextXPE() {
//        return elements == null || elements.length >= index + 1;
//    }
//
//
//    /**
//     * returns the next path-element.
//     *
//     * @return The next element or null of the end has been reached.
//     */
//    public XPathValidator getNextXPE() {
//        if (!hasNextXPE())
//            return null;
//
//        return elements[++index];
//    }
//
    
    
    /**
     * Sets the path string used to validate the psi-tree.
     *
     * @param pathStr The validation path as String.
     * @throws XPathSelectorException If the path is empty or malformed.
     */
    public void setPath(String pathStr) throws XPathSelectorException {
        paths = pathStr.split("/");
        
        if (isEmptyPath()) {
            elements = null;
            return;
        }
        
        elements = new XPathValidatorList(paths.length);
        
        for (var path : paths) {
            var elem = XPathValidator.fromString(path);
            if (elem == null)
                throw new XPathSelectorException("Malformed PisXPath element: '" + path + "'.");
            
            elements.add(elem);
        }
    }
    
    
    /**
     * Tests for an empty path.
     *
     * @return True if empty.
     */
    public boolean isEmptyPath() {
        return paths == null || paths.length == 0;
    }
    
    
    /**
     * Returns the length of the path-elements.
     *
     * @return Length as int.
     */
    public int length() {
        if (!isEmptyPath()) return paths.length;
        return -1;
    }
    
    
    /**
     * Recursive path resolver, tries to math the path against the psi-tree.
     *
     * @param psiElement     The psi-element to match.
     * @param xPathValidator The path-element to match against.
     * @return True if matches well.
     */
    private boolean resolve(PsiElement psiElement, XPathValidatorList validators) {
        
        // iterate over path elements
        for (var validator : validators) {
            // find matching children
            var children = validator.resolve(psiElement);
            var result = children.size() > 0;
            
            // iterate over all children matched by validator
            for (var child : children) {
                result = result && resolve(child, validators.tail());
            }
            
            return result;
        }
        
        
        //        var children = xPathValidator.resolve(psiElement);
//
//        // path end
//        if (!hasNextXPE()) {
//            return children != null && children.size() > 0;
//        }
//
//        var next = getNextXPE();
//
//        // try to resolve all sub-nodes ins psi-tree
//        for (var child : children) {
//            if (resolve(child, next))
//                return false;
//        }
//
//        return children.size() > 0;
        return false;
        
    }
    
    
    /**
     * Validate the given path against the given psi-tree.
     *
     * @param path The validation path.
     * @return True if the given path-expression matches the given psi-tree.
     */
    public boolean validate(String path) {
        try {
            setPath(path);
        } catch (XPathSelectorException e) {
            return false;
        }
        
        index = 0;
        return resolve(root, elements);
    }
    
    
}
