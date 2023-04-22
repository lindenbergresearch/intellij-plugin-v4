package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;

import java.awt.*;

/**
 *
 */
public class StyleProperties implements Cloneable {
    
    // the elements margin
    protected StyledElementMargin margin;
    
    // basic color setup
    protected JBColor foreground;
    protected JBColor background;
    protected JBColor outlineColor;
    protected JBColor textColor;
    protected JBColor labelColor;
    
    // flags
    protected boolean filled;
    
    // stroke properties
    protected Stroke stroke;
    
    // font-face
    protected Font textFont;
    protected Font labelFont;
    
    // round corner - horizontal arc diameter
    protected int arcDiameter;
    
    /* ----- CONSTRUCTOR -----------------------------------------------------------------------------*/
    
    
    public StyleProperties(StyledElementMargin margin, JBColor foreground, JBColor background, JBColor textColor, JBColor labelColor, Stroke stroke, Font textFont, boolean filled, int arcDiameter) {
        this.margin = margin;
        this.filled = filled;
        this.foreground = foreground;
        this.background = background;
        this.outlineColor = (JBColor) background.darker();
        this.textColor = filled ? textColor : background;
        this.labelColor = filled ? labelColor : background;
        this.stroke = stroke;
        this.textFont = textFont;
        this.labelFont = DefaultStyles.LABEL_FONT;
        this.arcDiameter = arcDiameter;
    }
    
    
    public StyleProperties(StyledElementMargin margin, JBColor foreground, JBColor background, JBColor outlineColor, JBColor textColor, JBColor labelColor, boolean filled, Stroke stroke, Font textFont, Font labelFont) {
        this.margin = margin;
        this.foreground = foreground;
        this.background = background;
        this.outlineColor = outlineColor;
        this.textColor = filled ? textColor : outlineColor;
        this.labelColor = filled ? labelColor : outlineColor;
        this.filled = filled;
        this.stroke = stroke;
        this.textFont = textFont;
        this.labelFont = labelFont;
    }
    
    
    /**
     * Sets the background color for this style by getting it
     * from the app setting.
     *
     * @param colorKey The color-key matching the style element.
     */
    public void setBackgroundFromColorKey(ColorKey colorKey) {
        var appSettings = ANTLRv4UISettingsState.getInstance();
        
        this.setBackground(
            new JBColor(appSettings.getColor(colorKey),
                appSettings.getColor(colorKey))
        );
    }
    
    
    /**
     * Sets the text color for this style by getting it
     * from the app setting.
     *
     * @param colorKey The color-key matching the style element.
     */
    public void setTextColorFromColorKey(ColorKey colorKey) {
        var appSettings = ANTLRv4UISettingsState.getInstance();
        
        this.setTextColor(
            new JBColor(appSettings.getColor(colorKey),
                appSettings.getColor(colorKey))
        );
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
    
    
    public JBColor getOutlineColor() {
        return outlineColor;
    }
    
    
    public void setOutlineColor(JBColor outlineColor) {
        this.outlineColor = outlineColor;
    }
    
    
    public JBColor getLabelColor() {
        return labelColor;
    }
    
    
    public void setLabelColor(JBColor labelColor) {
        this.labelColor = labelColor;
    }
    
    
    public boolean isFilled() {
        return filled;
    }
    
    
    public void setFilled(boolean filled) {
        this.filled = filled;
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
    
    
    public Font getTextFont() {
        return textFont;
    }
    
    
    public void setTextFont(Font textFont) {
        this.textFont = textFont;
    }
    
    
    public Font getLabelFont() {
        return labelFont;
    }
    
    
    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }
    
    
    public int getArcDiameter() {
        return arcDiameter;
    }
    
    
    public void setArcDiameter(int arcDiameter) {
        this.arcDiameter = arcDiameter;
    }
    
    /* ----- FACTORY METHODS -------------------------------------------------------------------------*/
    
    
    public static StyleProperties deriveFrom(StyleProperties styles) {
        return new StyleProperties(
            styles.margin,
            styles.foreground,
            styles.background,
            styles.textColor,
            styles.labelColor,
            styles.stroke,
            styles.textFont,
            styles.filled,
            styles.arcDiameter
        );
    }
    
    
    @Override
    public StyleProperties clone() {
        try {
            StyleProperties clone = (StyleProperties) super.clone();
            clone.setTextFont(getTextFont().deriveFont((float) getTextFont().getSize()));
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
