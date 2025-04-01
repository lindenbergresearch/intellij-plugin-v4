package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.components.PersistentStateComponent;

public interface ANTLRSettingsListener<T> {
    void settingsChanged(PersistentStateComponent<T> t);
}
