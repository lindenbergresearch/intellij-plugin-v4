
package org.antlr.intellij.plugin.configdialogs;


import com.intellij.openapi.options.Configurable;
import org.antlr.intellij.plugin.ANTLRUtils;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;


/**
 * Provides controller functionality for application settings.
 */
public class ANTLRv4UISettingsConfigurable implements Configurable {
    
    private ANTLRv4UISettingsComponent component;
    
    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP
    
    
    @Nls(capitalization = Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ANTLR Color Setting";
    }
    
    
    @Override
    public JComponent getPreferredFocusedComponent() {
        return component.getPreferredFocusedComponent();
    }
    
    
    @Nullable
    @Override
    public JComponent createComponent() {
        component = new ANTLRv4UISettingsComponent();
        return component.getPanel();
    }
    
    
    @Override
    public boolean isModified() {
        var settings = ANTLRv4UISettingsState.getInstance();
        
        for (var colorKey : ColorKey.VALUES) {
            var selectedColor = component.getSelectedColors(colorKey);
            var storedColor = settings.getColor(colorKey);
            if (!ANTLRUtils.compareJBColors(selectedColor, storedColor))
                return true;
            
            if (!Objects.equals(component.getSelectedState(colorKey), settings.getCheckBoxState(colorKey)))
                return true;
        }
        
        return
            settings.isAutoShowAntlrTool() != component.isAutoShow() ||
                settings.isEnableDebugConsole() != component.isDebugConsole() ||
                settings.isUseFractionalMetrics() != component.isFractionalMetrics() ||
                !Objects.equals(settings.getFontRegular(), component.getRegularFont()) ||
                !Objects.equals(settings.getFontMonospaced(), component.getMonospacedFont());
    }
    
    
    @Override
    public void apply() {
        var settings = ANTLRv4UISettingsState.getInstance();
        
        for (var colorKey : ColorKey.VALUES) {
            settings.setColor(colorKey, component.getSelectedColors(colorKey));
            settings.setCheckBoxState(colorKey, component.getSelectedState(colorKey));
        }
        
        settings.setAutoShowAntlrTool(component.isAutoShow());
        settings.setEnableDebugConsole(component.isDebugConsole());
        settings.setUseFractionalMetrics(component.isFractionalMetrics());
        
        settings.setFontRegular(component.getRegularFont());
        settings.setFontMonospaced(component.getMonospacedFont());
        
        ANTLRv4UISettingsState.notifyListeners();
    }
    
    
    @Override
    public void reset() {
        component.updateColorPanels();
        component.getPanel().invalidate();
    }
    
    
    @Override
    public void disposeUIResources() {
        component = null;
    }
}
