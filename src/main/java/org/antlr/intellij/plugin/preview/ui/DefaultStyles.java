package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import org.antlr.intellij.plugin.preview.ui.StyledText.HorizontalLayout;
import org.antlr.intellij.plugin.preview.ui.StyledText.VerticalLayout;

import java.awt.*;

/**
 * Style constants, colors and fonts
 */
public class DefaultStyles {

    /* ----- FONT FACES --------------------------------------*/

    public static final Font REGULAR_FONT =
        JBFont.regular().deriveFont(13.f);
    //   new Font("Helvetica Neue", Font.PLAIN, 13);

    public static final Font BOLD_FONT =
        REGULAR_FONT.deriveFont(Font.BOLD, 13.f);

    public static final Font ITALIC_FONT =
        REGULAR_FONT.deriveFont(Font.ITALIC, 13.f);

    public static final Font TERMINAL_FONT =
        // JBFont.getFont(JBFont.MONOSPACED).deriveFont(11.f);
        REGULAR_FONT.deriveFont(13.f);


    /* ----- COLORS ------------------------------------------*/

    public final static JBColor JB_COLOR_BRIGHT = new JBColor(new Color(232, 232, 233), new Color(1, 2, 3));
    public final static JBColor JB_COLOR_DARK = new JBColor(new Color(25, 24, 24), new Color(226, 227, 227));
    public final static JBColor JB_COLOR_BLUE = JBColor.BLUE;
    public final static JBColor JB_COLOR_RED = new JBColor(new Color(204, 80, 80), new Color(134, 72, 72));
    public final static JBColor JB_COLOR_GREEN = new JBColor(new Color(88, 204, 88), new Color(74, 145, 99));
    public final static JBColor JB_COLOR_YELLOW = new JBColor(new Color(182, 182, 61), new Color(175, 175, 92));
    public final static JBColor JB_COLOR_PINK = new JBColor(new Color(201, 85, 172), new Color(187, 66, 187));
    public final static JBColor JB_COLOR_CYAN = new JBColor(new Color(58, 192, 192), new Color(63, 132, 141));


    /* ----- CONSTANTS ---------------------------------------*/

    // round rectangle diameter
    public static final int ROUND_RECT_WIDTH = 10;
    public static final int ROUND_RECT_HEIGHT = 10;

    // text layout setup
    public static final HorizontalLayout
        HORIZONTAL_TEXT_LAYOUT = HorizontalLayout.CENTER;
    public static final VerticalLayout
        VERTICAL_TEXT_LAYOUT = VerticalLayout.MIDDLE;

    // default stroke setup
    public static final Stroke DEFAULT_STROKE =
        new BasicStroke(
            1.f,
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


    /* ----- DEFAULT STYLES ----------------------------------*/

    public static final StyleProperties DEFAULT_STYLE =
        new StyleProperties(
            StyledElementMargin.DEFAULT,
            JB_COLOR_BRIGHT,
            JB_COLOR_BLUE,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            REGULAR_FONT
        );

    public static final StyleProperties ERROR_NODE_STYLE =
        new StyleProperties(
            StyledElementMargin.DEFAULT,
            JB_COLOR_BRIGHT,
            JB_COLOR_RED,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            REGULAR_FONT
        );

    public static final StyleProperties ROOT_NODE_STYLE =
        new StyleProperties(
            StyledElementMargin.DEFAULT,
            JB_COLOR_BRIGHT,
            JB_COLOR_CYAN,
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            BOLD_FONT
        );

    public static final StyleProperties TERMINAL_NODE_STYLE =
        new StyleProperties(
            StyledElementMargin.DEFAULT,
            JB_COLOR_BRIGHT,
            JB_COLOR_GREEN,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            TERMINAL_FONT
        );

    public static final StyleProperties SELECTED_NODE_STYLE =
        new StyleProperties(
            StyledElementMargin.DEFAULT,
            JB_COLOR_BRIGHT,
            (JBColor) JB_COLOR_BLUE.brighter(),
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            REGULAR_FONT
        );
    /* ----- DEFAULT SHAPES ----------------------------------*/

}