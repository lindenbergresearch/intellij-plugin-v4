
package org.antlr.intellij.plugin.configdialogs;


import com.intellij.openapi.options.Configurable;
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
        System.out.println("createComponent()");
        
        component = new ANTLRv4UISettingsComponent();
        return component.getPanel();
    }
    
    
    @Override
    public boolean isModified() {
        var settings = ANTLRv4UISettingsState.getInstance();
        var modified = true;//!mySettingsComponent.getUserNameText().equals(settings.userId);
//        modified |= mySettingsComponent.getIdeaUserStatus() != settings.ideaStatus;
//        modified |= !Objects.equals(mySettingsComponent.getColor(), settings.color);
        
        return modified;
    }
    
    
    @Override
    public void apply() {
        System.out.println("apply()");
        var settings = ANTLRv4UISettingsState.getInstance();
    
        for(var colorKey : ColorKey.VALUES) {
            settings.setColor(colorKey, component.getSelectedColors(colorKey));
        }
    }
    
    
    @Override
    public void reset() {
        System.out.println("reset()");
        var settings = ANTLRv4UISettingsState.getInstance();
//        settings.setColor(ColorKey.VIEWER_BACKGROUND, DefaultStyles.getConsoleBackground());
//        settings.setColor(ColorKey.TEXT_COLOR, DefaultStyles.JB_COLOR_BRIGHT);
//        settings.setColor(ColorKey.CONNECTOR_COLOR, DefaultStyles.EDGE_COLOR_DEFAULT);
//        settings.setColor(ColorKey.ROOT_NODE_COLOR, DefaultStyles.ROOT_NODE_STYLE.getBackground());
    }
    
    
    @Override
    public void disposeUIResources() {
        component = null;
    }
    
}
