package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.JBColor;

import java.awt.*;

/**
 *
 */
public class StyleProperties {

    // the elements margin
    protected StyledElementMargin margin;

    // basic color setup
    protected JBColor foreground;
    protected JBColor background;
    protected JBColor textColor;

    // stroke properties
    protected Stroke stroke;

    // font-face
    protected Font font;


    /* ----- CONSTRUCTOR -----------------------------------------------------------------------------*/


    public StyleProperties(StyledElementMargin margin, JBColor foreground, JBColor background, JBColor textColor, Stroke stroke, Font font) {
        this.margin = margin;
        this.foreground = foreground;
        this.background = background;
        this.textColor = textColor;
        this.stroke = stroke;
        this.font = font;
    }


    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/


    public StyledElementMargin getMargin() {
        return margin;
    }


    public void setMargin(StyledElementMargin margin) {
        this.margin = margin;
    }


    public JBColor getForeground() {
        return foreground;
    }


    public void setForeground(JBColor foreground) {
        this.foreground = foreground;
    }


    public JBColor getBackground() {
        return background;
    }


    public void setBackground(JBColor background) {
        this.background = background;
    }


    public JBColor getTextColor() {
        return textColor;
    }


    public void setTextColor(JBColor textColor) {
        this.textColor = textColor;
    }


    public Stroke getStroke() {
        return stroke;
    }


    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }


    public Font getFont() {
        return font;
    }


    public void setFont(Font font) {
        this.font = font;
    }

    /* ----- FACTORY METHODS -------------------------------------------------------------------------*/


    public static StyleProperties deriveFrom(StyleProperties styles) {
        return new StyleProperties(styles.margin, styles.foreground, styles.background, styles.textColor, styles.stroke, styles.font);
    }


}
