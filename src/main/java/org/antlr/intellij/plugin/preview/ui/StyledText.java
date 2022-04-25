package org.antlr.intellij.plugin.preview.ui;

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
    public String text;


    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledText(String text) {
        super();
        this.text = text;
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

        Dimension bounds =
            UIHelper.getFullStringBounds(graphics2D, text);

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

        graphics2D.setColor(getTextColor());
        graphics2D.drawString(text, (float) x, (float) y);

//
//        graphics2D.setStroke(new BasicStroke(0.55f));
//        graphics2D.setColor(JBColor.MAGENTA);
//        graphics2D.drawLine((int) x, (int) y, (int) (x + bounds.getWidth()), (int) y);
//
//
//        graphics2D.setStroke(new BasicStroke(0.55f));
//        graphics2D.setColor(JBColor.RED);
//        graphics2D.drawRect(
//            (int) (getCenter().x - bounds.getWidth() * 0.5),
//            (int) (getCenter().y - bounds.getHeight() * 0.5),
//            (int) bounds.getWidth(),
//            (int) bounds.getHeight()
//        );
    }




    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/


}
