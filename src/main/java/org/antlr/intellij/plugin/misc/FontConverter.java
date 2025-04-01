package org.antlr.intellij.plugin.misc;

import com.intellij.util.ui.JBFont;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FontConverter extends Converter<JBFont> {
    
    @Override public @Nullable JBFont fromString(@NotNull String value) {
        return FontManager.getFont(value);
    }
    
    @Override public @Nullable String toString(@NotNull JBFont value) {
        return value.getFontName();
    }
}
