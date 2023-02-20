// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ui.ColorPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.antlr.intellij.plugin.misc.Tuple2;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.antlr.intellij.plugin.Utils.deconstructJBColor;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class ANTLRv4UISettingsComponent {
    // color panels stored by color-key.
    private final Map<ColorKey, Tuple2<ColorPanel, ColorPanel>> colorPanels
        = new LinkedHashMap<>();
    
    
    private JPanel mainPanel;
    
    private JCheckBox scrollToFirst;
    private JCheckBox scrollToLast;
    private final ANTLRv4UISettingsState appSettings;
    
    Insets emptyInsets = new JBInsets(0);
    Insets cpInsets = JBUI.insetsLeft(8);
    
    
    public ANTLRv4UISettingsComponent() {
        appSettings = ANTLRv4UISettingsState.getInstance();
        mainPanel = new JPanel(new BorderLayout());
        
        var commonSettingsPanel = new JPanel();
        commonSettingsPanel.setLayout(new BoxLayout(commonSettingsPanel, BoxLayout.Y_AXIS));
        commonSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Parse-Tree Color Settings"));
        
        
        scrollToFirst = new JCheckBox("Scroll to first?");
        scrollToLast = new JCheckBox("Scroll to last?");
        // ...
        
        commonSettingsPanel.add(scrollToFirst);
        commonSettingsPanel.add(scrollToLast);
        
        mainPanel = add(mainPanel, commonSettingsPanel);
        
        /*|--------------------------------------------------------------------------|*/
        
        var colorsPanel = new JPanel(new GridBagLayout());
        colorsPanel.setBorder(IdeBorderFactory.createTitledBorder("Parse-Tree Color Settings"));
        
        mainPanel = add(mainPanel, colorsPanel);
        
        
        var constraints =
            new GridBagConstraints(
                0, 0, 1, 1, 0, 0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                emptyInsets,
                0, 0
            );
        
        var i = 1;
        
        constraints.gridx = 0;
        constraints.gridy = i;
        constraints.weightx = 0.3;
        constraints.ipady = 30;
        constraints.insets = emptyInsets;
        
        var label = new JLabel("");
        colorsPanel.add(label, constraints);
        
        constraints.gridx = 1;
        constraints.gridy = i;
        constraints.weightx = 0.1;
        constraints.insets = emptyInsets;
        
        colorsPanel.add(new JLabel("IntelliJ Light"), constraints);
        
        
        constraints.gridx = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 0.2;
        constraints.insets = emptyInsets;
        constraints.ipady = 0;
        
        colorsPanel.add(new JLabel("IntelliJ Darcula"), constraints);
        
        i++;
        
        addColorPanelComponent(colorsPanel, "Background", constraints, ColorKey.VIEWER_BACKGROUND, i++);
        addColorPanelComponent(colorsPanel, "Foreground", constraints, ColorKey.VIEWER_FOREGROUND, i++);
        addColorPanelComponent(colorsPanel, "Text Color", constraints, ColorKey.TEXT_COLOR, i++);
        
        addColorPanelComponent(colorsPanel, "Default Node", constraints, ColorKey.DEFAULT_NODE_BACKGROUND, i++);
        addColorPanelComponent(colorsPanel, "Root Node", constraints, ColorKey.ROOT_NODE_COLOR, i++);
        addColorPanelComponent(colorsPanel, "Terminal Node", constraints, ColorKey.TERMINAL_NODE_COLOR, i++);
        addColorPanelComponent(colorsPanel, "EOF Node", constraints, ColorKey.EOF_NODE_COLOR, i++);
        
        addColorPanelComponent(colorsPanel, "ERROR Node", constraints, ColorKey.ERROR_COLOR, i++);
        addColorPanelComponent(colorsPanel, "Resync Node", constraints, ColorKey.RESYNC_COLOR, i++);
        
        addColorPanelComponent(colorsPanel, "Connector", constraints, ColorKey.CONNECTOR_COLOR, i++);
        addColorPanelComponent(colorsPanel, "Connector Selected", constraints, ColorKey.CONNECTOR_SELECTED_COLOR, i++);
        
    }
    
    
    /**
     * Adds a row of two {@link ColorPanel} components for a {@link JBColor} including a label.
     *
     * @param target      Target {@link JPanel} to be added.
     * @param label       Text label.
     * @param constraints Constraints used.
     * @param colorKey    The corresponding color-key.
     * @param row         The corresponding row.
     */
    private void addColorPanelComponent(JPanel target, String label, GridBagConstraints constraints, ColorKey colorKey, int row) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.1;
        constraints.insets = emptyInsets;
        
        target.add(new JLabel(label), constraints);
        
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 0.1;
        constraints.insets = cpInsets;
        
        var color = appSettings.getColor(colorKey);
        
        var panels = getOrCreate(colorKey);
        
        var colorTuple = deconstructJBColor(color);
        panels.a.setSelectedColor(colorTuple.first());
        panels.b.setSelectedColor(colorTuple.second());
        
        target.add(panels.first(), constraints);
        
        constraints.gridx = 2;
        constraints.weightx = 0.9;
        constraints.insets = cpInsets;
        
        target.add(panels.second(), constraints);
    }
    
    
    /**
     * Adds a component to a {@link JPanel} with {@link BorderLayout} and returns a new {@link JPanel}.
     *
     * @param panel     The source panel.
     * @param component The component to add.
     * @return The new panel including the old panel and component.
     */
    private JPanel add(JPanel panel, JComponent component) {
        var p = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.SOUTH);
        p.add(panel, BorderLayout.NORTH);
        
        return p;
    }
    
    
    /**
     * Returns or creates a new ColorPanels Tuple2 and adds it to the map.
     *
     * @param colorKey The color-key to be bound.
     * @return The found panels or created.
     */
    private Tuple2<ColorPanel, ColorPanel> getOrCreate(ColorKey colorKey) {
        if (colorPanels.containsKey(colorKey))
            return colorPanels.get(colorKey);
        
        var panels = new Tuple2<>(
            new ColorPanel(),
            new ColorPanel()
        );
        
        colorPanels.put(colorKey, panels);
        
        return panels;
    }
    
    
    /**
     * Returns the selected colors for this color-key.
     *
     * @param colorKey The corresponding color-key.
     * @return The color which has been set.
     */
    public JBColor getSelectedColors(ColorKey colorKey) {
        var tuple = colorPanels.get(colorKey);
        
        if (tuple == null || tuple.isNull()) return
            DefaultStyles.getDefaultColor(colorKey);
        
        
        return new JBColor(
            Objects.requireNonNull(tuple.first().getSelectedColor()),
            Objects.requireNonNull(tuple.second().getSelectedColor())
        );
    }
    
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }
    
}
