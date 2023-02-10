package org.antlr.intellij.plugin.preview;

import com.intellij.ui.JBColor;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.antlr.intellij.plugin.preview.ui.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.awt.RenderingHints.*;

/**
 *
 */
public class JInfoLabel extends JComponent {
    public static final double LINE_HEIGHT_FACTOR = 1.32;
    public static final double LABEL_WIDTH_FACTOR = 1.12;
    
    private double lineHeight = 0;
    
    private Dimension labelSize;
    private Dimension textSize;
    
    
    private Point offset = new Point(10, 10);
    
    private float fontSize = DefaultStyles.BASIC_FONT_SIZE - 1;
    
    private JBColor labelColor = DefaultStyles.JB_COLOR_GRAY;
    private JBColor textColor = DefaultStyles.JB_COLOR_DARK;
    
    protected final Map<String, InfoLabelElement<?>> content = new LinkedHashMap<>();
    
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public JInfoLabel() {
        setLayout(null);
        setSize(10, 10);
        setLocation(offset);
        setOpaque(false);
        setBackground(DefaultStyles.JB_COLOR_TRANSPARENT);
        setFont(DefaultStyles.MONOSPACE_FONT.deriveFont(fontSize));
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public void addLabelElement(String fieldName, InfoLabelElement<?> element) {
        if (element != null)
            content.put(fieldName, element);
    }
    
    
    public Map<String, InfoLabelElement<?>> getContent() {
        return content;
    }
    
    
    public void setColors(JBColor labelColor, JBColor textColor) {
        this.labelColor = labelColor;
        this.textColor = textColor;
    }
    
    
    public Point getOffset() {
        return offset;
    }
    
    
    public void setOffset(Point offset) {
        this.offset = offset;
    }
    
    
    public float getFontSize() {
        return fontSize;
    }
    
    
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }
    
    
    public <T> void updateElement(String fieldName, T value) {
        if (content.containsKey(fieldName)) {
            var e = content.get(fieldName);
            ((InfoLabelElement<T>) e).setValue(value);
        }
    }
    
    
    public void updateElement(String fieldName, String text) {
        if (content.containsKey(fieldName)) {
            var e = content.get(fieldName);
            e.setText(text);
        }
    }
    
    
    /**
     * Returns the maximum width needed by all label elements.
     *
     * @param g2d Graphics context.
     * @return Max width in px.
     */
    private double getMaxLabelWidth(Graphics2D g2d) {
        var width = 0.0;
        
        for (var infoLabelElement : content.values()) {
            var dim = UIHelper.getFullStringBounds(g2d, infoLabelElement.getLabel());
            width = Math.max(width, dim.getWidth());
        }
        
        return width;
    }
    
    
    /**
     * Returns the maximum width needed by all text elements.
     *
     * @param g2d Graphics context.
     * @return Max width in px.
     */
    private double getMaxTextWidth(Graphics2D g2d) {
        var width = 0.0;
        
        for (var infoLabelElement : content.values()) {
            var dim = UIHelper.getFullStringBounds(g2d, infoLabelElement.getDisplayText());
            width = Math.max(width, dim.getWidth());
        }
        
        return width;
    }
    
    
    /**
     * @param g2
     */
    private void updateMetrics(Graphics2D g2) {
        lineHeight = fontSize * LINE_HEIGHT_FACTOR;
        
        labelSize = new Dimension(
            (int) (getMaxLabelWidth(g2) * LABEL_WIDTH_FACTOR),
            (int) (lineHeight * content.size())
        );
        
        textSize = new Dimension(
            (int) getMaxTextWidth(g2),
            (int) (lineHeight * content.size())
        );
        
        
        var infoSize = new Dimension(
            labelSize.width + textSize.width,
            (int) (lineHeight * content.size())
        );
        
        
        setSize(infoSize);
    }
    
    
    /**
     * @param g
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        var g2 = (Graphics2D) g;
        
        // anti-alias the lines
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        
        // set fractional metrics ON to improve text rendering quality
        g2.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        
        // anti-alias text, default aa
        g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
        
        //   g2.setFont(font);
        
        updateMetrics(g2);

//            g2.setColor(labelColor);
//            g2.drawRect(0, 0, (int) infoWidth-1, (int) infoHeight-1);
        
        var yOffset = lineHeight;
        for (var infoLabelElement : content.values()) {
            g2.setColor(labelColor);
            g2.drawString(infoLabelElement.getLabel(), 0, (int) yOffset);
            
            g2.setColor(textColor);
            g2.drawString(infoLabelElement.getDisplayText(), labelSize.width, (int) yOffset);
            
            yOffset += lineHeight;
        }
        
        
    }
    
}
