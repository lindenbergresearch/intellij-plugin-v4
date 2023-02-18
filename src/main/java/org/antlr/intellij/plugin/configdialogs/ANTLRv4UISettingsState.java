package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.JBColor;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.antlr.intellij.plugin.Utils;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ANTLR Color and Tree-Viewer settings persistence.
 */
@State(
    name = "org.intellij.sdk.settings.AppSettingsState",
    storages = @Storage(value = "ANTLRSettings.xml")
)
public class ANTLRv4UISettingsState implements PersistentStateComponent<ANTLRv4UISettingsState> {
    public static final JBColor DEFAULT_FALLBACK_COLOR = JBColor.BLUE;
    
    
    /**
     * Color keys corresponding to each color element.
     */
    public enum ColorKey {
        VIEWER_BACKGROUND,
        VIEWER_FOREGROUND,
        TEXT_COLOR,
        DEFAULT_NODE_FOREGROUND,
        DEFAULT_NODE_BACKGROUND,
        ROOT_NODE_COLOR,
        EOF_NODE_COLOR,
        CONNECTOR_COLOR,
        CONNECTOR_SELECTED_COLOR,
        TERMINAL_NODE_COLOR,
        ERROR_COLOR,
        RESYNC_COLOR;
        
        public static final ColorKey[] VALUES = values();
    }
    
    
    // color storage
    public final Map<ColorKey, String> colors = new LinkedHashMap<>();
    
    /*|--------------------------------------------------------------------------|*/
    
    
    /**
     * Put a  {@link JBColor} color to storage.
     *
     * @param colorKey The corresponding color-key.
     * @param color    The color to store.
     */
    public void setColor(@NotNull ColorKey colorKey, JBColor color) {
        colors.put(colorKey, Utils.toHexJBColor(color));
    }
    
    
    /**
     * Get {@link JBColor} from storage via it's color-key.
     *
     * @param colorKey The corresponding color-key.
     * @return The stored color for this color-key or a default if not found.
     */
    public JBColor getColor(@NotNull ColorKey colorKey) {
        if (!colors.containsKey(colorKey))
            // return default color if not found in config
            return DefaultStyles.getDefaultColor(colorKey);
        
        var colorHex = colors.get(colorKey);
        
        try {
            return Utils.hexToJBColor(colorHex);
        } catch (NumberFormatException ex) {
            // default on malformed string
            return DefaultStyles.getDefaultColor(colorKey);
        }
    }
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public static ANTLRv4UISettingsState getInstance() {
        return ApplicationManager.getApplication().getService(ANTLRv4UISettingsState.class);
    }
    
    
    @Nullable
    @Override
    public ANTLRv4UISettingsState getState() {
        return this;
    }
    
    
    @Override
    public void loadState(@NotNull ANTLRv4UISettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
}
