package org.antlr.intellij.plugin.psi.xpath;

public class XPathSelectorException extends Exception {
    /**
     *
     */
    public XPathSelectorException() {
        super();
    }
    
    
    /**
     * @param message
     */
    public XPathSelectorException(String message) {
        super(message);
    }
    
    
    /**
     * @param message
     * @param cause
     */
    public XPathSelectorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    
    /**
     * @param cause
     */
    public XPathSelectorException(Throwable cause) {
        super(cause);
    }
    
    
    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    protected XPathSelectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
