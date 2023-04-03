package org.antlr.intellij.plugin.preview.ui;

import java.awt.*;
import java.awt.geom.Dimension2D;

/**
 *
 */
public class DoubleDimension2D extends Dimension2D {
    public final static DoubleDimension2D ZERO = new DoubleDimension2D(0, 0);
    
    /**
     * Internal vars.
     */
    private double width, height;
    
    
    /**
     * @param width
     * @param height
     */
    public DoubleDimension2D(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    
    /**
     * @return
     */
    public Dimension toDimension() {
        return new Dimension(
            Math.round((float) width),
            Math.round((float) height)
        );
    }
    
    
    /**
     * @param factor
     * @return
     */
    public DoubleDimension2D scale(double factor) {
        return new DoubleDimension2D(
            getWidth() * factor,
            getHeight() * factor
        );
    }
    
    
    /**
     * @param d1
     * @param d2
     * @return
     */
    public static DoubleDimension2D max(DoubleDimension2D d1, DoubleDimension2D d2) {
        return new DoubleDimension2D(
            Math.max(d1.width, d2.width),
            Math.max(d1.height, d2.height)
        );
    }
    
    
    /**
     * @param d1
     * @param d2
     * @return
     */
    public void max(DoubleDimension2D d1) {
        width = Math.max(d1.width, this.width);
        height = Math.max(d1.height, this.height);
    }
    
    
    /**
     * @param d1
     * @param d2
     * @return
     */
    public static DoubleDimension2D min(DoubleDimension2D d1, DoubleDimension2D d2) {
        return new DoubleDimension2D(
            Math.min(d1.width, d2.width),
            Math.min(d1.height, d2.height)
        );
    }
    
    
    /**
     * @param d1
     * @param d2
     * @return
     */
    public void min(DoubleDimension2D d1) {
        width = Math.min(d1.width, this.width);
        height = Math.min(d1.height, this.height);
    }
    
    
    /**
     * @return
     */
    public boolean isZero() {
        return width <= 0 && height <= 0;
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public DoubleDimension2D plus(double other) {
        return new DoubleDimension2D(width + other, height + other);
    }
    
    
    public DoubleDimension2D plus(DoubleDimension2D other) {
        return new DoubleDimension2D(width + other.width, height + other.height);
    }
    
    
    public DoubleDimension2D minus(double other) {
        return new DoubleDimension2D(width - other, height - other);
    }
    
    
    public DoubleDimension2D minus(DoubleDimension2D other) {
        return new DoubleDimension2D(width - other.width, height - other.height);
    }
    
    
    public DoubleDimension2D mul(double other) {
        return new DoubleDimension2D(width * other, height * other);
    }
    
    
    public DoubleDimension2D mul(DoubleDimension2D other) {
        return new DoubleDimension2D(width * other.width, height * other.height);
    }
    
    
    public DoubleDimension2D div(double other) {
        return new DoubleDimension2D(width / other, height / other);
    }
    
    
    public DoubleDimension2D div(DoubleDimension2D other) {
        return new DoubleDimension2D(width / other.width, height / other.height);
    }
    
    
    public DoubleDimension2D half() {
        return new DoubleDimension2D(width / 2, height / 2);
    }
    
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public boolean eitherZero() {
        return width * height <= 0;
    }
    
    
    @Override
    public double getWidth() {
        return width;
    }
    
    
    @Override
    public double getHeight() {
        return height;
    }
    
    
    @Override
    public void setSize(double v, double v1) {
        width = v;
        height = v1;
    }
    
    
    @Override
    public String toString() {
        return "[" + width + "px, " + height + "px]";
    }
}
