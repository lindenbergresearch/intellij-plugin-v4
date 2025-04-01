package org.antlr.intellij.plugin.misc;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FontInfo;
import com.intellij.util.ui.JBFont;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manage all fonts.
 */
public class FontManager {
    /**
     * Bundle of Font and Label for UI components.
     */
    @Getter @Setter
    public static class FontBundle {
        JBFont font;
        JBLabel label;
        Boolean monospaced;
        static double maxWidth;
    }
    
    /* ----------------------------------------------------------------------- */
    
    // font-cache
    private static final Map<String, FontBundle> bundles = new HashMap<>();
    
    // logger instance
    public static final Logger LOG = Logger.getInstance(FontManager.class);
    
    // the default font / fallback font
    public static final JBFont DEFAULT_FONT = JBFont.regular();
    public static final String FONTS_PATH = "/fonts/";
    
    
    /* ----------------------------------------------------------------------- */
    
    
    static {
        addSystemFonts();
    }
    
    /* ----------------------------------------------------------------------- */
    
    
    private static void setMaxWidth(double with) {
        if (FontBundle.maxWidth < with) {
            FontBundle.maxWidth = with;
        }
    }
    
    
    public static int getMaximumWidth() {
        return (int) Math.round(FontBundle.maxWidth);
    }
    
    /* ----------------------------------------------------------------------- */
    
    
    public static JBFont[] getFontsArray() {
        List<JBFont> list = new ArrayList<>();
        for (var fontBundle : bundles.values()) {
            var font = fontBundle.getFont();
            list.add(font);
        }
        
        return list.toArray(new JBFont[0]);
    }
    
    
    public static JBLabel[] getLabelsArray() {
        List<JBLabel> list = new ArrayList<>();
        for (var fontBundle : bundles.values()) {
            var label = fontBundle.getLabel();
            list.add(label);
        }
        
        return list.toArray(new JBLabel[0]);
    }
    
    
    public static FontBundle[] getBundles() {
        return getSortedFontBundles()
            .values()
            .toArray(new FontBundle[0]);
    }
    
    
    public static FontBundle[] getMonospacedBundles() {
        return getSortedFontBundles()
            .values()
            .stream()
            .filter(bundle -> bundle.getMonospaced())
            .toArray(value -> new FontBundle[value]);
    }
    
    
    public static Map<String, FontBundle> getSortedFontBundles() {
        return bundles.entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> entry.getValue().getFont().getFontName()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new // bewahrt Sortierreihenfolge
            ));
    }
    
    
    public static JBLabel getLabel(@NotNull JBFont font) {
        for (var fontBundle : bundles.values()) {
            if (fontBundle.getFont().equals(font)) {
                return fontBundle.getLabel();
            }
        }
        
        return null;
    }
    
    
    /**
     * Registers and adds a new Font to the bundle cache.
     *
     * @param font The Font to be registered in cache.
     */
    public static void registerFont(@NotNull JBFont font, boolean monospaced) {
        var bundle = new FontBundle();
        bundle.setFont(font);
        bundle.setMonospaced(monospaced);
        
        var label = new JBLabel(font.getFontName());
        label.setFont(font);
        bundle.setLabel(label);
        
        var frc = new FontRenderContext(null, true, true);
        var bounds = font.getStringBounds(font.getFontName(), frc);
        setMaxWidth(bounds.getWidth());
        
        bundles.put(font.getFontName(), bundle);
    }
    
    
    /**
     * Add all system fonts to cache.
     */
    public static void addSystemFonts() {
        for (var fontInfo : FontInfo.getAll(true)) {
            registerFont(JBFont.create(fontInfo.getFont()), fontInfo.isMonospaced());
        }
    }
    
    
    /**
     * Loads a font from the resources-directory.
     *
     * @param name     The path to the font file within the JAR archive.
     * @param fontSize The desired font size.
     * @return The loaded font.
     */
    public static Font loadFont(String name, float fontSize) {
        // test for given path prefix for the fonts folder or add it
        
        var resourcePath = name;
        
        if (!resourcePath.startsWith(FONTS_PATH)) {
            resourcePath = FONTS_PATH + resourcePath;
        }
        
        // test for suffix
        if (!resourcePath.endsWith(".ttf")) {
            resourcePath += ".ttf";
        }
        
        // look for cached font
        if (bundles.containsKey(name)) {
            LOG.info("Font already loaded: " + name + " -> using cached: " + bundles.get(name).getFont().getFontName());
            return bundles.get(name).getFont().deriveFont(fontSize);
        }
        
        try (var is = FontManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOG.warn("Font file not found: " + resourcePath);
                return DEFAULT_FONT;
            }
            
            var font = (JBFont) JBFont.createFont(JBFont.TRUETYPE_FONT, is).deriveFont(JBFont.PLAIN, fontSize);
            registerFont(font, false);
            LOG.info("Loaded font: " + '[' + name + ']' + resourcePath);
            return font;
        } catch (FontFormatException e) {
            LOG.warn("Font format error: " + resourcePath, e);
            return DEFAULT_FONT;
        } catch (IOException e) {
            LOG.warn("Font file not found: " + resourcePath, e);
            return DEFAULT_FONT;
        }
    }
    
    
    /**
     * Checks if a font is already loaded.
     *
     * @param name The font resource path.
     * @return True if the font is loaded, false otherwise.
     */
    public static boolean isFontLoaded(@NotNull String name) {
        return bundles.containsKey(name);
    }
    
    
    /**
     * Retrieves a loaded font by its name / resource path.
     *
     * @param name The fonts name / resource path.
     * @return The loaded font, or null if not found.
     */
    public static JBFont getFont(@NotNull String name) {
        if (!isFontLoaded(name)) {
            return DEFAULT_FONT;
        }
        
        return bundles.get(name).getFont();
    }
    
    
    /**
     * Searches a bundle by the given name.
     *
     * @param name The Name of the font.
     * @return The resulting bundle.
     */
    public static FontBundle getBundle(@NotNull String name) {
        return bundles.get(name);
    }
    
    
    /**
     * Returns the number of loaded fonts.
     *
     * @return The number of loaded fonts.
     */
    public static int getLoadedFontCount() {
        return bundles.size();
    }
    
    
    /**
     * Removes a font from the cache.
     *
     * @param name The font resource path to remove.
     */
    public static void removeFont(@NotNull String name) {
        bundles.remove(name);
    }
    
    
    /**
     * Clears all cached fonts.
     */
    public static void clearFontCache() {
        bundles.clear();
    }
    
    
    /**
     * Returns the "full" bounds of a given string including the descent.
     *
     * @param graphics2D Graphics context.
     * @param s          String to measure.
     * @param font       Font to measure.
     * @return Bounds as double dimension.
     */
    public static Dimension getFullStringBounds(Graphics2D graphics2D, String s, Font font) {
        if (graphics2D == null) {
            return new Dimension(0, 0);
        }
        
        var fm = graphics2D.getFontMetrics(font);
        var bounds = new Dimension();
        
        bounds.setSize(
            fm.stringWidth(s),                  // width
            fm.getAscent() + fm.getDescent()    // height = ascending plus descending
        );
        
        return bounds;
    }
    
    
    /**
     * Returns the "full" bounds of a given string including the descent.
     * Using the current font set at graphics-context.
     *
     * @param graphics2D Graphics context.
     * @param s          String to measure.
     * @return Bounds as double dimension.
     */
    public static Dimension getFullStringBounds(Graphics2D graphics2D, String s) {
        return getFullStringBounds(graphics2D, s, graphics2D.getFont());
    }
    
    
    /**
     * Computes the origin of a string based on a given point defining the center.
     *
     * @param graphics2D Graphics context.
     * @param center     Point defining the center.
     * @param s          String to draw.
     */
    public static Point2D.Double getStringCentered(Graphics2D graphics2D, Point2D.Double center, String s) {
        var fm = graphics2D.getFontMetrics(graphics2D.getFont());
        var bounds = getFullStringBounds(graphics2D, s);
        
        return new Point2D.Double(
            center.x - bounds.getWidth() / 2.,
            center.y + fm.getAscent() / 2. - fm.getDescent() / 2.
        );
    }
    
    
    /**
     * Draws a string based on a given point defining the center.
     *
     * @param graphics2D Graphics context.
     * @param center     Point defining the center.
     * @param s          String to draw.
     */
    public static void drawStringCentered(Graphics2D graphics2D, Point2D.Double center, String s) {
        var origin = getStringCentered(graphics2D, center, s);
        graphics2D.drawString(s, (float) origin.x, (float) origin.y);
    }
}
