package org.antlr.intellij.plugin.preview.ui;


import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.antlr.intellij.plugin.preview.ui.StyledText.HorizontalLayout;
import org.antlr.intellij.plugin.preview.ui.StyledText.VerticalLayout;

import java.awt.*;

/**
 * Style constants, colors and fonts
 */
public class DefaultStyles {
    /* ----- CONSTANTS ---------------------------------------*/
    
    // round rectangle arc diameter
    public static final int DEFAULT_ARC_DIAMETER = 7;
    
    
    // scale factor for footer in labels
    public static final float LABEL_FOOTER_FONT_SCALE = 0.8f;
    
    // filled flags
    public static final Boolean ELEMENT_FILLED = true;
    public static final Boolean ELEMENT_OUTLINED_ONLY = false;
    
    
    // text layout setup
    public static final HorizontalLayout
        HORIZONTAL_TEXT_LAYOUT = HorizontalLayout.CENTER;
    public static final VerticalLayout
        VERTICAL_TEXT_LAYOUT = VerticalLayout.MIDDLE;
    
    
    /* ----- MARGIN ------------------------------------------*/
    
    public static final StyledElementMargin
        DEFAULT_TEXT_MARGIN = new StyledElementMargin(5);
    
    public static final StyledElementMargin
        DEFAULT_MARGIN = new StyledElementMargin(0);
    
    public static final StyledElementMargin
        ROOT_NODE_MARGIN = new StyledElementMargin(9);
    
    public static final StyledElementMargin
        EOF_NODE_MARGIN = new StyledElementMargin(9);
    
    public static final StyledElementMargin
        RESYNC_NODE_MARGIN = new StyledElementMargin(5);
    
    public static final StyledElementMargin
        TERMINAL_NODE_MARGIN = new StyledElementMargin(5);
    
    
    /* ----- FONT FACES --------------------------------------*/
    
    public static final Font BaseFont =
        UIHelper.createTrueType("inter/Inter-Medium", new StyledElementMargin());
    
    public static final Font BaseFontBold =
        UIHelper.createTrueType("inter/Inter-Bold", new StyledElementMargin());
    
    public static final Font BaseFontItalic =
        UIHelper.createTrueType("inter/Inter-Italic", new StyledElementMargin());
    
    // --- common font-sizes
    
    public static final float BASIC_FONT_SIZE =
        JBFont.regular().getSize2D() + 2;
    
    public static final float BASIC_LABEL_FONT_SIZE =
        BASIC_FONT_SIZE * LABEL_FOOTER_FONT_SCALE;
    
    public static final float BASIC_TERMINAL_FONT_SIZE =
        BASIC_FONT_SIZE - 2;
    
    public static final float BASIC_TERMINAL_LABEL_FONT_SIZE =
        BASIC_TERMINAL_FONT_SIZE * LABEL_FOOTER_FONT_SCALE;
    
    // --- fonts
    
    public static final Font BASIC_FONT =
        BaseFont.deriveFont(BASIC_FONT_SIZE);
    
    public static final Font SMALL_FONT =
        BaseFont.deriveFont(BASIC_FONT_SIZE - 2);
    
    public static final Font BOLD_FONT =
        BaseFontBold.deriveFont(Font.BOLD, BASIC_FONT_SIZE);
    
    public static final Font ITALIC_FONT =
        BaseFontItalic.deriveFont(Font.ITALIC, BASIC_FONT_SIZE);
    
    public static final Font LABEL_FONT =
        BaseFont.deriveFont(BASIC_LABEL_FONT_SIZE);
    
    public static final Font MONOSPACE_FONT =
        new Font("Monospaced", Font.PLAIN, (int) BASIC_FONT_SIZE);
    
    public static final Font TERMINAL_FONT =
        MONOSPACE_FONT.deriveFont(Font.BOLD, BASIC_TERMINAL_FONT_SIZE);
    
    public static final Font TERMINAL_LABEL_FONT =
        MONOSPACE_FONT.deriveFont(Font.BOLD, BASIC_TERMINAL_LABEL_FONT_SIZE);
    
    public static final Font SMALL_ITALIC_TERMINAL_FONT =
        MONOSPACE_FONT.deriveFont(Font.BOLD | Font.ITALIC, BASIC_FONT_SIZE - 2);
    
    
    /**
     * Returns the bold variant of a given font.
     *
     * @param font Source font.
     * @return Resulting font.
     */
    public static Font bold(Font font) {
        return font.deriveFont(Font.BOLD, font.getSize());
    }
    
    
    /**
     * Returns the italic variant of a given font.
     *
     * @param font Source font.
     * @return Resulting font.
     */
    public static Font italic(Font font) {
        return font.deriveFont(Font.ITALIC, font.getSize());
    }
    
    
    /**
     * Returns the bold and italic variant of a given font.
     *
     * @param font Source font.
     * @return Resulting font.
     */
    public static Font boldItalic(Font font) {
        return bold(italic(font));
    }
    
    
    /**
     * Scales a font by the given factor.
     *
     * @param font   Font to scale.
     * @param factor Scaling factor.
     * @return Derived font scaled by factor.
     */
    public static Font getScaledFont(Font font, float factor) {
        var size = ((float) font.getSize()) * factor;
        return font.deriveFont(size);
    }
    
    /* ----- COLORS ------------------------------------------*/
    
    public final static JBColor JB_COLOR_BRIGHT = new JBColor(new Color(232, 232, 233), new Color(1, 2, 3));
    public final static JBColor JB_COLOR_GRAY = new JBColor(Gray._122, new Color(100, 102, 103));
    public final static JBColor JB_COLOR_DARK_GRAY = new JBColor(Gray._50, new Color(60, 87, 63));
    public final static JBColor JB_COLOR_DARK = new JBColor(new Color(25, 24, 24), new Color(226, 227, 227));
    public final static JBColor JB_COLOR_BLUE = JBColor.BLUE;
    public final static JBColor JB_COLOR_RED = new JBColor(new Color(204, 80, 80), new Color(231, 87, 87));
    public final static JBColor JB_COLOR_BRIGHT_RED = new JBColor(new Color(173, 35, 35), new Color(255, 35, 35));
    public final static JBColor JB_COLOR_GREEN = new JBColor(new Color(54, 126, 54), new Color(101, 255, 94));
    public final static JBColor JB_COLOR_YELLOW = new JBColor(new Color(255, 242, 97), new Color(248, 248, 105));
    public final static JBColor JB_COLOR_PINK = new JBColor(new Color(201, 85, 172), new Color(187, 66, 187));
    public final static JBColor JB_COLOR_CYAN = new JBColor(new Color(58, 167, 192), new Color(90, 199, 170));
    public final static JBColor JB_COLOR_BROWN = new JBColor(new Color(245, 176, 106), new Color(203, 85, 42));
    
    public final static JBColor JB_COLOR_TRANSPARENT = new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));
    
    public final static JBColor EDGE_COLOR_DEFAULT = JB_COLOR_GRAY;
    public final static JBColor EDGE_COLOR_SELECTED = JB_COLOR_DARK;
    
    public final static JBColor NODE_OUTLINE_COLOR = new JBColor(new Color(255, 255, 255, 120), new Color(0, 0, 0, 120));
    
    
    /**
     * Default console background color.
     *
     * @return Background color as JBColor.
     */
    public static JBColor getConsoleBackground() {
        return new JBColor(
            JBColor.background(),
            JBColor.background().darker().darker()
        );
    }
    
    /* ----- STROKES -----------------------------------------*/
    
    public static final Stroke DEFAULT_STROKE =
        new BasicStroke(
            1.0f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    public static final Stroke THIN_STROKE =
        new BasicStroke(
            0.85f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    public static final Stroke THICK_STROKE =
        new BasicStroke(
            2.1f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    public static final Stroke HUGE_STROKE =
        new BasicStroke(
            3.5f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    public static final Stroke EDGE_STROKE_DEFAULT =
        new BasicStroke(
            1.5f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    public static final Stroke EDGE_STROKE_SELECTED =
        new BasicStroke(
            2.f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        );
    
    
    
    /* ----- DEFAULT STYLES ----------------------------------*/
    
    /*|--- DEFAULT NODES ---|*/
    
    public static final StyleProperties DEFAULT_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_BLUE,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            BASIC_FONT,
            ELEMENT_FILLED,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getDefaultNodeStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            getColorFromAppSettings(ColorKey.DEFAULT_NODE_BACKGROUND),
            getColorFromAppSettings(ColorKey.TEXT_COLOR),
            DEFAULT_STROKE,
            BASIC_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.DEFAULT_NODE_BACKGROUND),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- ERROR NODES ---|*/
    
    public static final StyleProperties ERROR_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_RED,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            BASIC_FONT,
            ELEMENT_OUTLINED_ONLY,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getDefaultErrorStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            getColorFromAppSettings(ColorKey.ERROR_COLOR),
            getColorFromAppSettings(ColorKey.TEXT_COLOR),
            DEFAULT_STROKE,
            BASIC_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.ERROR_COLOR),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- RESYNC NODES ---|*/
    
    public static final StyleProperties RESYNC_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_RED,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            BASIC_FONT,
            ELEMENT_OUTLINED_ONLY,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getDefaultResyncStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            getColorFromAppSettings(ColorKey.RESYNC_COLOR),
            getColorFromAppSettings(ColorKey.TEXT_COLOR),
            DEFAULT_STROKE,
            BASIC_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.RESYNC_COLOR),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- ROOT NODES ---|*/
    
    public static final StyleProperties ROOT_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            JB_COLOR_CYAN,
            JB_COLOR_BRIGHT,
            HUGE_STROKE,
            BASIC_FONT,
            ELEMENT_FILLED,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getDefaultRootStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            getColorFromAppSettings(ColorKey.ROOT_NODE_COLOR),
            getColorFromAppSettings(ColorKey.TEXT_COLOR),
            HUGE_STROKE,
            BASIC_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.ROOT_NODE_COLOR),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- END OF FILE NODES ---|*/
    
    public static final StyleProperties EOF_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            JB_COLOR_BROWN,
            JB_COLOR_BRIGHT,
            DEFAULT_STROKE,
            TERMINAL_FONT,
            ELEMENT_FILLED,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getEOFNodeStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            getColorFromAppSettings(ColorKey.EOF_NODE_COLOR),
            getColorFromAppSettings(ColorKey.TEXT_COLOR),
            DEFAULT_STROKE,
            TERMINAL_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.EOF_NODE_COLOR),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- TERMINAL NODES ---|*/
    
    public static final StyleProperties TERMINAL_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            JB_COLOR_GRAY,
            JB_COLOR_DARK,
            DEFAULT_STROKE,
            TERMINAL_FONT,
            ELEMENT_OUTLINED_ONLY,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getTerminalNodeStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            getColorFromAppSettings(ColorKey.TERMINAL_NODE_COLOR),
            (JBColor) getColorFromAppSettings(ColorKey.TERMINAL_NODE_COLOR).brighter().brighter().brighter(),
            THIN_STROKE,
            TERMINAL_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.TERMINAL_NODE_COLOR),
            DEFAULT_ARC_DIAMETER
        
        );
    }
    
    /*|--- SELECTED NODES ---|*/
    
    public static final StyleProperties SELECTED_NODE_STYLE =
        new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_BRIGHT,
            (JBColor) JB_COLOR_BLUE.brighter(),
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            BASIC_FONT,
            ELEMENT_FILLED,
            DEFAULT_ARC_DIAMETER
        );
    
    
    public static StyleProperties getSelectedNodeStyle() {
        return new StyleProperties(
            DEFAULT_MARGIN,
            JB_COLOR_DARK,
            (JBColor) getColorFromAppSettings(ColorKey.DEFAULT_NODE_BACKGROUND).brighter(),
            JB_COLOR_BRIGHT,
            THICK_STROKE,
            BASIC_FONT,
            getCheckBoxStateFromAppSettings(ColorKey.DEFAULT_NODE_BACKGROUND),
            DEFAULT_ARC_DIAMETER
        );
    }
    
    /* ----- DEFAULT STYLES WITH SAVED VALUES ---------------------------*/
    
    
    /**
     * Returns the default color by color-key.
     *
     * @param colorKey The color key to match.
     * @return The matching JBColor.
     */
    public static JBColor getDefaultColor(ColorKey colorKey) {
        switch (colorKey) {
            case VIEWER_BACKGROUND:
                return getConsoleBackground();
            case TEXT_COLOR:
                return DEFAULT_STYLE.textColor;
            case DEFAULT_NODE_BACKGROUND:
                return DEFAULT_STYLE.background;
            case EOF_NODE_COLOR:
                return EOF_NODE_STYLE.background;
            case ROOT_NODE_COLOR:
                return ROOT_NODE_STYLE.background;
            case TERMINAL_NODE_COLOR:
                return TERMINAL_NODE_STYLE.background;
            case ERROR_COLOR:
            case RESYNC_COLOR:
                return ERROR_NODE_STYLE.background;
            case CONNECTOR_COLOR:
                return EDGE_COLOR_DEFAULT;
            case CONNECTOR_SELECTED_COLOR:
                return EDGE_COLOR_SELECTED;
            
            default:
                return JB_COLOR_BRIGHT;
        }
    }
    
    
    public static Boolean getDefaultCheckBoxState(ColorKey colorKey) {
        switch (colorKey) {
            case DEFAULT_NODE_BACKGROUND:
                return DEFAULT_STYLE.filled;
            case EOF_NODE_COLOR:
                return EOF_NODE_STYLE.filled;
            case ROOT_NODE_COLOR:
                return ROOT_NODE_STYLE.filled;
            case TERMINAL_NODE_COLOR:
                return TERMINAL_NODE_STYLE.filled;
            case ERROR_COLOR:
            case RESYNC_COLOR:
                return ERROR_NODE_STYLE.filled;
            
            default:
                return false;
        }
    }
    
    
    public static Boolean getCheckBoxStateFromAppSettings(ColorKey colorKey) {
        var appSettings = ANTLRv4UISettingsState.getInstance();
        return appSettings.getCheckBoxState(colorKey);
    }
    
    
    public static JBColor getColorFromAppSettings(ColorKey colorKey) {
        var appSettings = ANTLRv4UISettingsState.getInstance();
        return appSettings.getColor(colorKey);
    }
}
