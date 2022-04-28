package org.antlr.intellij.plugin.preview.ui;

import com.intellij.ui.JBColor;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public abstract class StyledElement implements StyleRendering, StyleSetup {

    /* ----- BASE PROPERTIES -------------------------------------------------------------------------*/

    // parent element for inheritance of styles
    protected StyledElement parent;

    // the assigned area to draw
    protected Rectangle2D viewport;

    // style properties
    protected StyleProperties styleProperties;

    /**
     * All styled elements to draw.
     */
    protected final List<StyledElement> elements = new ArrayList<>();

    // indicates if the element is active
    protected boolean enabled = true;

    /* ----- CONSTRUCTOR -----------------------------------------------------------------------------*/


    /**
     * Empty constructor (properties may inherit by getter/setter).
     */
    public StyledElement() {
        setup();
    }


    /**
     * Constructs a new StyledElement with its basic setup.
     *
     * @param parent   The parent element.
     * @param viewport The elements' area to draw to.
     * @param styles   The style-properties of the element.
     */
    public StyledElement(StyledElement parent, Rectangle2D viewport, StyleProperties styles) {
        this.parent = parent;
        this.viewport = viewport;
        this.styleProperties = styles;

        setup();
    }



    /* ----- RENDERING / EVENTS ----------------------------------------------------------------------*/


    /**
     * Called before content rendering.
     */
    @Override
    public void onBeforeRendering() {}


    /**
     * Called after content rendering done.
     */
    @Override
    public void onAfterRendering() {}


    /**
     * This is the actual place where the user-code
     * for drawing the styled element can be put.
     *
     * @param graphics2D Graphics context.
     */
    public abstract void draw(Graphics2D graphics2D);


    /**
     * Rendering method to be implemented in subclasses.
     *
     * @param graphics2D Graphics context.
     */
    @Override
    public final void render(Graphics2D graphics2D) {
        if (graphics2D == null) return;

        /* raise event before rendering starts */
        onBeforeRendering();

        /* delegate rendering to styled text elements */
        for (StyledElement element : elements) {
            element.render(graphics2D);
        }

        // call draw method only if enabled
        if (enabled) {
            // take care of the margin which may have been set
            StyledElementMargin margin =
                getMargin() == null ?
                    new StyledElementMargin() :
                    getMargin();


            // translate origin to local viewport of the element
            graphics2D.translate(
                getViewport().getX() + margin.getLeft(),
                getViewport().getY() + margin.getTop()
            );

            applyStyle(graphics2D);
            draw(graphics2D);

            // restore origin
            graphics2D.translate(
                -getViewport().getX(),
                -getViewport().getY()
            );
        }

        /* raise event after rendering has been finished */
        onAfterRendering();
    }


    /**
     * Adds a styled Element to the list of child elements.
     *
     * @param styledElement Child styled element.
     */
    public void add(StyledElement styledElement) {
        if (styledElement != null) {
            styledElement.setParent(this);
            elements.add(styledElement);
        }
    }


    /**
     * Applies the current style to the given graphics context.
     *
     * @param graphics2D Graphic context.
     */
    public void applyStyle(Graphics2D graphics2D) {
        if (getFont() != null) graphics2D.setFont(getFont());
        if (getStroke() != null) graphics2D.setStroke(getStroke());
        if (getForeground() != null) graphics2D.setColor(getForeground());
    }


    /* ----- STANDARD GETTER / SETTER ----------------------------------------------------------------*/


    /**
     * Returns the absolute center of the elements draw area.
     * (Don't care of margins.)
     *
     * @return
     */
    public Point2D.Double getCenter() {
        return new Point2D.Double(
            getWidth() * 0.5,
            getHeight() * 0.5
        );
    }


    /**
     * @return
     */
    public Dimension2D getArea() {
        if (getViewport() != null)
            return getViewport().getBounds().getSize();

        return null;
    }


    /**
     * @return
     */
    public Point2D getOrigin() {
        if (getViewport() != null)
            return new Point2D.Double(
                getViewport().getX(),
                getViewport().getY()
            );

        return null;
    }


    public Rectangle2D getViewport() {
        if (viewport == null && parent != null)
            return parent.getViewport();

        return viewport;
    }


    public void setViewport(Rectangle2D viewport) {
        this.viewport = viewport;
    }


    /**
     * Set viewport by scalar values.
     *
     * @param x      Location X
     * @param y      Location Y
     * @param width  Width
     * @param height Height
     */
    public void setViewport(float x, float y, float width, float height) {
        setViewport(new Rectangle2D.Float(x, y, width, height));
    }


    /**
     * Moves the viewport relative by a given point.
     *
     * @param offset Offset as Point.
     */
    public void shiftViewport(Point2D offset) {
        setViewport(
            (float) (getViewport().getX() + offset.getX()),
            (float) (getViewport().getY() + offset.getY()),
            (float) (getViewport().getWidth() - offset.getX()),
            (float) (getViewport().getHeight() - offset.getY())
        );
    }


    /**
     * Moves the viewport relative by a gives pair of X, Y coordinates.
     *
     * @param x
     * @param y
     */
    public void shiftViewport(float x, float y) {
        shiftViewport(new Point2D.Float(x, y));
    }


    /**
     * Returns the actual width taking care of margin.
     *
     * @return
     */
    public double getWidth() {
        if (getMargin() != null)
            return getViewport().getWidth() +
                getStyleProperties().getMargin().getLeft() +
                getStyleProperties().getMargin().getRight();

        else return getViewport().getWidth();
    }


    /**
     * Returns the actual height taking care of margin.
     *
     * @return
     */
    public double getHeight() {
        if (getMargin() != null)
            return getViewport().getHeight() +
                getStyleProperties().getMargin().getTop() +
                getStyleProperties().getMargin().getBottom();

        return getViewport().getHeight();
    }


    public JBColor getForeground() {
        if (getStyleProperties().getForeground() == null && parent != null)
            return parent.getForeground();

        return getStyleProperties().getForeground();
    }


    public void setForeground(JBColor foreground) {
        this.getStyleProperties().setForeground(foreground);
    }


    public JBColor getBackground() {
        if (getStyleProperties().getBackground() == null && parent != null)
            return parent.getBackground();

        return getStyleProperties().getBackground();
    }


    public void setBackground(JBColor background) {
        this.getStyleProperties().setBackground(background);
    }


    public JBColor getTextColor() {
        if (getStyleProperties().getTextColor() == null && parent != null)
            return parent.getTextColor();

        return getStyleProperties().getTextColor();
    }


    public void setTextColor(JBColor textColor) {
        this.getStyleProperties().setTextColor(textColor);
    }


    public Stroke getStroke() {
        if (getStyleProperties().getStroke() == null && parent != null)
            return parent.getStroke();

        return getStyleProperties().getStroke();
    }


    public void setStroke(Stroke stroke) {
        this.getStyleProperties().setStroke(stroke);
    }


    public boolean hasParent() {
        return parent != null;
    }


    public Font getFont() {
        if (getStyleProperties().getFont() == null && parent != null)
            return parent.getFont();

        return getStyleProperties().getFont();
    }


    public void setFont(Font font) {
        this.getStyleProperties().setFont(font);
    }


    public StyledElementMargin getMargin() {
        if (getStyleProperties().getMargin() == null && parent != null) {
            return parent.getMargin();
        }

        return getStyleProperties().getMargin();
    }


    public void setMargin(StyledElementMargin margin) {
        this.getStyleProperties().setMargin(margin);
    }


    public StyledElement getParent() {
        return parent;
    }


    public void setParent(StyledElement parent) {
        this.parent = parent;
    }


    public boolean isEnabled() {
        return enabled;
    }


    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    public StyleProperties getStyleProperties() {
        if (styleProperties == null && parent != null) {
            return parent.styleProperties;
        }

        return styleProperties;
    }


    public void setStyleProperties(StyleProperties styleProperties) {
        this.styleProperties = styleProperties;
    }


    public List<StyledElement> getElements() {
        return elements;
    }
}
