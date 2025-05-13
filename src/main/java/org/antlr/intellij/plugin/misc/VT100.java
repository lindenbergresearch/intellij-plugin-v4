package org.antlr.intellij.plugin.misc;

import java.io.PrintStream;

/**
 * VT100 is a utility class for building ANSI-styled console output
 * using a fluent Builder pattern. It supports VT100-compatible escape codes
 * for styling terminal text with colors and formats.
 */
public class VT100 {
    
    // --- ANSI escape code constants
    public static final String RESET = "\033[0m";
    /* ------------------------------------------------------------------------- */
    
    // --- Foreground colors
    public static final String BLACK = "\033[30m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";
    public static final String BRIGHT_BLACK = "\033[90m";
    public static final String BRIGHT_RED = "\033[91m";
    public static final String BRIGHT_GREEN = "\033[92m";
    public static final String BRIGHT_YELLOW = "\033[93m";
    public static final String BRIGHT_BLUE = "\033[94m";
    public static final String BRIGHT_MAGENTA = "\033[95m";
    public static final String BRIGHT_CYAN = "\033[96m";
    public static final String BRIGHT_WHITE = "\033[97m";
    /* ------------------------------------------------------------------------- */
    
    // --- Background colors
    public static final String BG_BLACK = "\033[40m";
    public static final String BG_RED = "\033[41m";
    public static final String BG_GREEN = "\033[42m";
    public static final String BG_YELLOW = "\033[43m";
    public static final String BG_BLUE = "\033[44m";
    public static final String BG_MAGENTA = "\033[45m";
    public static final String BG_CYAN = "\033[46m";
    public static final String BG_WHITE = "\033[47m";
    public static final String BG_BRIGHT_BLACK = "\033[100m";
    public static final String BG_BRIGHT_RED = "\033[101m";
    public static final String BG_BRIGHT_GREEN = "\033[102m";
    public static final String BG_BRIGHT_YELLOW = "\033[103m";
    public static final String BG_BRIGHT_BLUE = "\033[104m";
    public static final String BG_BRIGHT_MAGENTA = "\033[105m";
    public static final String BG_BRIGHT_CYAN = "\033[106m";
    public static final String BG_BRIGHT_WHITE = "\033[107m";
    /* ------------------------------------------------------------------------- */
    
    // --- Text formatting
    public static final String BOLD = "\033[1m";
    public static final String ITALIC = "\033[3m";
    public static final String UNDERLINE = "\033[4m";
    public static final String STRIKETHROUGH = "\033[9m";
    public static final String REVERSED = "\033[7m";
    /* ------------------------------------------------------------------------- */
    
    
    /**
     * Builder class for fluent creation of VT100-formatted strings.
     */
    public static class Builder {
        private final StringBuilder sb = new StringBuilder();
        private boolean autoReset = true;
        
        
        /**
         * Adds a foreground color escape code.
         *
         * @param color ANSI escape code for foreground color.
         * @return Builder instance.
         */
        public Builder foreground(String color) {
            sb.append(color);
            return this;
        }
        
        
        /**
         * Adds a background color escape code.
         *
         * @param color ANSI escape code for background color.
         * @return Builder instance.
         */
        public Builder background(String color) {
            sb.append(color);
            return this;
        }
        
        
        /**
         * Adds bold style to the output.
         *
         * @return Builder instance.
         */
        public Builder bold() {
            sb.append(BOLD);
            return this;
        }
        
        
        /**
         * Adds italic style to the output.
         *
         * @return Builder instance.
         */
        public Builder italic() {
            sb.append(ITALIC);
            return this;
        }
        
        
        /**
         * Adds underline style to the output.
         *
         * @return Builder instance.
         */
        public Builder underline() {
            sb.append(UNDERLINE);
            return this;
        }
        
        
        /**
         * Adds strikethrough style to the output.
         *
         * @return Builder instance.
         */
        public Builder strikethrough() {
            sb.append(STRIKETHROUGH);
            return this;
        }
        
        
        /**
         * Adds reverse style (swap foreground and background).
         *
         * @return Builder instance.
         */
        public Builder reverse() {
            sb.append(REVERSED);
            return this;
        }
        
        
        /**
         * Appends the given text with current styling applied.
         *
         * @param text the text to display.
         * @return Builder instance.
         */
        public Builder text(String text) {
            sb.append(text);
            return this;
        }
        
        
        /**
         * Appends a manual reset (clear all styles).
         *
         * @return Builder instance.
         */
        public Builder reset() {
            sb.append(RESET);
            return this;
        }
        
        
        /**
         * Appends a (system dependent) new-line character.
         *
         * @return Builder instance.
         */
        public Builder NewLine() {
            sb.append(System.lineSeparator());
            return this;
        }
        
        
        /**
         * Enables or disables automatic reset at the end of rendering.
         *
         * @param enabled true to enable, false to disable.
         * @return Builder instance.
         */
        public Builder autoReset(boolean enabled) {
            this.autoReset = enabled;
            return this;
        }
        
        
        /**
         * Prints the rendered string to the given PrintStream.
         *
         * @param printStream The stream to print to.
         */
        public void printTo(PrintStream printStream) {
            if (printStream != null) {
                printStream.print(render());
            } else {
                throw new RuntimeException("Invalid PrintStream passed to VT100 renderer.");
            }
        }
        
        
        /**
         * Builds and returns the formatted string.
         * If autoReset is enabled, the string will be reset at the end.
         *
         * @return styled console output string.
         */
        public String render() {
            if (autoReset) {
                return sb + RESET;
            }
            
            return sb.toString();
        }
    }
    
    /* ------------------------------------------------------------------------------------------------------------------ */
    
    
    /**
     * Creates a new Builder instance for VT100 console formatting.
     *
     * @return Builder instance.
     */
    public static Builder create() {
        return new Builder();
    }
}