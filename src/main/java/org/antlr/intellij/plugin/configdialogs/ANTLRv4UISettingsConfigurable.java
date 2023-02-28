
package org.antlr.intellij.plugin.configdialogs;


import com.intellij.openapi.options.Configurable;
import org.antlr.intellij.plugin.Utils;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


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
            if (!Utils.compareJBColors(selectedColor, storedColor))
                return true;
        }
        
        return false;
    }
    
    
    @Override
    public void apply() {
        var settings = ANTLRv4UISettingsState.getInstance();
        
        for (var colorKey : ColorKey.VALUES) {
            settings.setColor(colorKey, component.getSelectedColors(colorKey));
        }
    }
    
    
    @Override
    public void reset() {
        component.reloadolors();
        component.getPanel().invalidate();
    }
    
    
    @Override
    public void disposeUIResources() {
        component = null;
    }
    
}
