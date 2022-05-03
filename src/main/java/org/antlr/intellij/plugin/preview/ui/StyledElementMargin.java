package org.antlr.intellij.plugin.preview.ui;

/**
 *
 */
public class StyledElementMargin {
    
    private double left, top, right, bottom;
    
    
    /**
     * Constructs a margin with all values set to zero.
     */
    public StyledElementMargin() {}
    
    
    /**
     * Constructs a margin with the given values.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public StyledElementMargin(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    
    /**
     * Constructs a margin via a given universal value.
     *
     * @param margin
     */
    public StyledElementMargin(double margin) {
        left = margin;
        top = margin;
        right = margin;
        bottom = margin;
    }
    
    
    public double getVertical() {
        return top + bottom;
    }
    
    
    public double getHorizonal() {
        return left + right;
    }
    
    
    public double getLeft() {
        return left;
    }
    
    
    public void setLeft(double left) {
        this.left = left;
    }
    
    
    public double getTop() {
        return top;
    }
    
    
    public void setTop(double top) {
        this.top = top;
    }
    
    
    public double getRight() {
        return right;
    }
    
    
    public void setRight(double right) {
        this.right = right;
    }
    
    
    public double getBottom() {
        return bottom;
    }
    
    
    public void setBottom(double bottom) {
        this.bottom = bottom;
    }
}



