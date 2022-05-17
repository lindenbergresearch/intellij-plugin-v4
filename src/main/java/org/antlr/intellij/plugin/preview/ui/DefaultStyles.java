package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import org.antlr.intellij.plugin.preview.ui.StyledText.HorizontalLayout;
import org.antlr.intellij.plugin.preview.ui.StyledText.VerticalLayout;

import java.awt.*;

/**
 * Style constants, colors and fonts
 */
public class DefaultStyles {
    /* ----- CONSTANTS ---------------------------------------*/
    // round rectangle diameter
    public static final int ROUND_RECT_WIDTH = 8;
    public static final int ROUND_RECT_HEIGHT = 8;
    
    // standard font size
    public static final float BASIC_FONT_SIZE = JBFont.regular().getSize2D();
    
    // text layout setup
    public static final HorizontalLayout
        HORIZONTAL_TEXT_LAYOUT = HorizontalLayout.CENTER;
    public static final VerticalLayout
        VERTICAL_TEXT_LAYOUT = VerticalLayout.MIDDLE;
    
    /* ----- MARGIN ------------------------------------------*/
    
    public static final StyledElementMargin
        DEFAULT_TEXT_MARGIN = new StyledElementMargin(5, 5, 5, 5);
    
    public static final StyledElementMargin
        DEFAULT_MARGIN = new StyledElementMargin(0, 0, 0, 0);
    
    public static final StyledElementMargin
        BIG_MARGIN = new StyledElementMargin(5, 5, 5, 5);
    
    public static final StyledElementMargin
        TERMINAL_MARGIN = new StyledElementMargin(5, 5, 5, 5);
    
    
    /* ----- FONT FACES --------------------------------------*/
    
    public static final Font REGULAR_FONT =
        JBFont.regular();
//        JBFont.regular().deriveFont(BASIC_FONT_SIZE);
    //   new Font("Times New Romen", Font.PLAIN, (int) BASIC_FONT_SIZE);
    
    public static final Font SMALL_FONT =
        JBFont.small();
    
    public static final Font BOLD_FONT =
        REGULAR_FONT.deriveFont(Font.BOLD, BASIC_FONT_SIZE);
    
    public static final Font ITALIC_FONT =
        REGULAR_FONT.deriveFont(Font.ITALIC, BASIC_FONT_SIZE);
    
    public static final Font MONOSPACE_FONT =
        new Font("Monospaced", Font.PLAIN, (int) BASIC_FONT_SIZE);
    
    public static final Font SMALL_TERMINAL_FONT =
        MONOSPACE_FONT.deriveFont(Font.BOLD, BASIC_FONT_SIZE - 2);
    
    public static final Font SMALL_ITALIC_TERMINAL_FONT =
        MONOSPACE_FONT.deriveFont(Font.BOLD | Font.ITALIC, BASIC_FONT_SIZE - 2);
    
    
    /* ----- COLORS ------------------------------------------*/
    
    public final static JBColor JB_COLOR_BRIGHT = new JBColor(new Color(232, 232, 233), new Color(1, 2, 3));
    public final static JBColor JB_COLOR_GRAY = new JBColor(Gray._122, new Color(100, 102, 103));
    public final static JBColor JB_COLOR_DARK = new JBColor(new Color(25, 24, 24), new Color(226, 227, 227));
    public final static JBColor JB_COLOR_BLUE = JBColor.BLUE;
    public final static JBColor JB_COLOR_RED = new JBColor(new Color(204, 80, 80), new Color(231, 87, 87));
    public final static JBColor JB_COLOR_GREEN = new JBColor(new Color(100, 197, 100), new Color(58, 110, 56));
    public final static JBColor JB_COLOR_YELLOW = new JBColor(new Color(255, 242, 97), new Color(248, 248, 105));
    public final static JBColor JB_COLOR_PINK = new JBColor(new Color(201, 85, 172), new Color(187, 66, 187));
    public final static JBColor JB_COLOR_CYAN = new JBColor(new Color(58, 167, 192), new Color(90, 199, 170));
    public final static JBColor JB_COLOR_BROWN = new JBColor(new Color(245, 176, 106), new Color(203, 85, 42));
    
    public final static JBColor EDGE_COLOR_DEFAULT = JB_COLOR_GRAY;
    public final static JBColor EDGE_COLOR_SELECTED = JB_COLOR_DARK;
    
    
    /**
     * Default console background color.
     *
     * @return
     */
    public static JBColor getConsoleBackground() {
//        if (JBColor.isBright()) {
//            return new JBColor(JBColor.background(), JBColor.background());
//        }
        
        return new JBColor(JBColor.background(), JBColor.background().darker().darker());
    }
    
    /* ----- STROKES -----------------------------------------*/
    
    // default stroke setup
    public static final Stroke DEFAULT_STROKE =
        new BasicStroke(
            1.0f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    // default stroke setup
    public static final Stroke THIN_STROKE =
        new BasicStroke(
            0.6f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    // default stroke setup
    public static final Stroke THICK_STROKE =
        new BasicStroke(
            2.1f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    // default stroke setup
    public static final Stroke EDGE_STROKE_DEFAULT =
        new BasicStroke(
            1.5f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    // default stroke setup
    public static final Stroke EDGE_STROKE_SELECTED =
        new BasicStroke(
            2.f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    
    
    /* ----- DEFAULT STYLES ----------------------------------*/
    
    public static final StyleProperties DEFAULT_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_BLUE,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            REGULAR_FONT
        );
    
    public static final StyleProperties ERROR_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_RED,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            REGULAR_FONT
        );
    
    public static final StyleProperties ROOT_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_CYAN,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            BOLD_FONT
        );
    
    public static final StyleProperties EOF_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            JB_COLOR_BROWN,
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            SMALL_ITALIC_TERMINAL_FONT
        );
    
    
    public static final StyleProperties TERMINAL_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            JB_COLOR_DARK,
            JB_COLOR_DARK,
            DEFAULT_STROKE,
            SMALL_TERMINAL_FONT
        );
    
    public static final StyleProperties SELECTED_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            (JBColor) JB_COLOR_BLUE.brighter(),
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            REGULAR_FONT
        );
}
