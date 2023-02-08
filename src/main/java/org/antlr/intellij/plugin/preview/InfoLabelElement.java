package org.antlr.intellij.plugin.preview;

/**
 *
 */
class InfoLabelElement<T> {
    private String label;
    private String text;
    private T value;
    private boolean isNumeric = true;
    
    
    public InfoLabelElement(String label, String text, T value) {
        this.label = label;
        this.text = text;
        this.value = value;
        
    }
    
    
    public InfoLabelElement(String label, T value) {
        this.label = label;
        this.value = value;
        this.text = "%.2f";
    }
    
    
    public InfoLabelElement(String label, String text) {
        this.label = label;
        this.text = text;
        isNumeric = false;
    }
    
    
    public T getValue() {
        return value;
    }
    
    
    public void setValue(T value) {
        this.value = value;
    }
    
    
    public String getDisplayText() {
        if (isNumeric) {
            return String.format(text, value);
        }
        
        return text;
    }
    
    
    public String getLabel() {
        return label;
    }
    
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    
    public String getText() {
        return text;
    }
    
    
    public void setText(String text) {
        this.text = text;
    }
}
