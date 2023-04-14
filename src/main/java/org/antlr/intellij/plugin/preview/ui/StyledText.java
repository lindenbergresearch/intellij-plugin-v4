package org.antlr.intellij.plugin.preview.ui;

import com.intellij.ui.JBColor;

import java.awt.*;
import java.awt.geom.Rectangle2D;


/**
 * Styled text element.
 */
public abstract class StyledText extends StyledElement {
    
    /* ----- LAYOUT CONSTANTS ------------------------------------------------------------------------*/
    
    
    /**
     * Horizontal text-layout regime.
     */
    enum HorizontalLayout {
        LEFT, CENTER, RIGHT
    }
    
    
    /**
     * Vertical text-layout regime.
     */
    enum VerticalLayout {
        TOP, MIDDLE, BOTTOM
    }
    
    
    /* ----- CONFIG ATTRIBUTES -----------------------------------------------------------------------*/
    protected VerticalLayout verticalTextLayout;
    protected HorizontalLayout horizontalTextLayout;
    protected String text;
    protected boolean isLabel = false;
    
    
    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledText(String text) {
        super();
        this.text = text;
    }
    
    
    public String getText() {
        return text;
    }
    
    
    public void setText(String text) {
        this.text = text;
    }
    
    
    public void setBold() {
        setTextFont(getTextFont().deriveFont(Font.BOLD));
    }
    
    
    public void setItalic() {
        setTextFont(getTextFont().deriveFont(Font.ITALIC));
    }
    
    
    public boolean isLabel() {
        return isLabel;
    }
    
    
    public void setLabel(boolean label) {
        isLabel = label;
    }
    
    
    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledText(StyledElement parent, Rectangle2D viewport, StyleProperties styles, String text) {
        super(parent, viewport, styles);
        this.text = text;
    }
    
    
    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public void draw(Graphics2D graphics2D) {
        if (text == null) text = "null";
        
        graphics2D.setFont(isLabel ? getLabelFont() : getTextFont());
        
        var bounds =
            UIHelper.getFullStringBounds(graphics2D, text, isLabel ? getLabelFont() : getTextFont());
        
        double x;
        switch (horizontalTextLayout) {
            case CENTER:
                x = UIHelper.getStringCentered(graphics2D, getCenter(), text).x;
                break;
            case RIGHT:
                x = getWidth() - bounds.getWidth();
                break;
            case LEFT:
            default:
                x = 0;
        }
        
        double y;
        switch (verticalTextLayout) {
            case TOP:
                y = bounds.getHeight();
                break;
            case BOTTOM:
                y = getHeight();
                break;
            case MIDDLE:
                y = UIHelper.getStringCentered(graphics2D, getCenter(), text).y;
                break;
            default:
                y = 0;
        }
        
        
        if (debug()) {
            graphics2D.setStroke(new BasicStroke(0.55f));
            graphics2D.setColor(JBColor.ORANGE.brighter());
            graphics2D.drawLine((int) x, (int) y, (int) (x + bounds.getWidth()), (int) y);
            
            Stroke dashed = new BasicStroke(
                0.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[]{2},
                0
            );
            
            graphics2D.setStroke(dashed);
            graphics2D.setColor(JBColor.RED);
            graphics2D.drawRect(
                (int) (getCenter().x - bounds.getWidth() * 0.5),
                (int) (getCenter().y - bounds.getHeight() * 0.5),
                (int) bounds.getWidth(),
                (int) bounds.getHeight()
            );
            
            graphics2D.setStroke(new BasicStroke(0.5f));
            graphics2D.setColor(JBColor.MAGENTA.brighter());
            
            graphics2D.drawLine(
                (int) 0.0,
                (int) getCenter().y,
                (int) getWidth(),
                (int) getCenter().y
            );
            
            graphics2D.drawLine(
                (int) getCenter().x,
                (int) 0.0,
                (int) getCenter().x,
                (int) getHeight()
            );
        }
        
        graphics2D.setColor(isLabel ? getLabelColor() : getTextColor());
        graphics2D.drawString(text, (float) x, (float) y);
    }
    
    
    
    
    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/
}
