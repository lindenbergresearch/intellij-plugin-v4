// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ui.ColorPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
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
    
    private final Map<ColorKey, Checkbox> stateCheckBoxes
        = new LinkedHashMap<>();
    
    private JPanel mainPanel;
    
    private final JCheckBox checkBox1;
    private final JCheckBox checkBox2;
    private final ANTLRv4UISettingsState appSettings;
    
    Insets emptyInsets = new JBInsets(0);
    Insets cpInsets = JBUI.insetsLeft(8);
    
    
    /**
     *
     */
    public ANTLRv4UISettingsComponent() {
        appSettings = ANTLRv4UISettingsState.getInstance();
        mainPanel = new JPanel(new BorderLayout());
        
        var commonSettingsPanel = new JPanel();
        commonSettingsPanel.setLayout(new BoxLayout(commonSettingsPanel, BoxLayout.Y_AXIS));
        commonSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Common Viewer Settings"));
        
        
        checkBox1 = new JBCheckBox("Automatically bring preview window in front when switching a grammar.");
        checkBox2 = new JBCheckBox("This is a second option.");
        // ...
        
        commonSettingsPanel.add(checkBox1);
        commonSettingsPanel.add(checkBox2);
        
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
        
        var label = new JBLabel("");
        colorsPanel.add(label, constraints);
        
        constraints.gridx = 1;
        constraints.weightx = 0.1;
        constraints.insets = emptyInsets;
        
        var l1 = new JBLabel("IntelliJ light");
        colorsPanel.add(l1, constraints);
        l1.invalidate();
        
        
        constraints.gridx = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 0.1;
        constraints.insets = emptyInsets;
        constraints.ipady = 0;
        
        var l2 = new JBLabel("IntelliJ darcula");
        colorsPanel.add(l2, constraints);
        l2.invalidate();
        
        constraints.gridx = 3;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.insets = emptyInsets;
        constraints.ipady = 0;
        
        var l3 = new JBLabel("Node is filled");
        colorsPanel.add(l3, constraints);
        l3.invalidate();
        
        i++;
        
        addColorPanelComponent(colorsPanel, "Background", constraints, ColorKey.VIEWER_BACKGROUND, i++, true);
        addColorPanelComponent(colorsPanel, "Foreground", constraints, ColorKey.VIEWER_FOREGROUND, i++, true);
        addColorPanelComponent(colorsPanel, "Text Color", constraints, ColorKey.TEXT_COLOR, i++, true);
        addColorPanelComponent(colorsPanel, "Label Color", constraints, ColorKey.LABEL_COLOR, i++, true);
        
        addColorPanelComponent(colorsPanel, "Default Node", constraints, ColorKey.DEFAULT_NODE_BACKGROUND, i++, false);
        addColorPanelComponent(colorsPanel, "Root Node", constraints, ColorKey.ROOT_NODE_COLOR, i++, false);
        addColorPanelComponent(colorsPanel, "Terminal Node", constraints, ColorKey.TERMINAL_NODE_COLOR, i++, false);
        addColorPanelComponent(colorsPanel, "EOF Node", constraints, ColorKey.EOF_NODE_COLOR, i++, false);
        
        addColorPanelComponent(colorsPanel, "ERROR Node", constraints, ColorKey.ERROR_COLOR, i++, false);
        addColorPanelComponent(colorsPanel, "Resync Node", constraints, ColorKey.RESYNC_COLOR, i++, false);
        
        addColorPanelComponent(colorsPanel, "Connector", constraints, ColorKey.CONNECTOR_COLOR, i++, true);
        addColorPanelComponent(colorsPanel, "Connector Selected", constraints, ColorKey.CONNECTOR_SELECTED_COLOR, i++, true);
        
        constraints.gridx = 0;
        constraints.gridy = i;
        constraints.gridwidth = 2;
        constraints.weightx = 0.2;
        constraints.insets = emptyInsets;
        constraints.ipady = 2;
        
        var resetButton = new JButton("Reset Colors");
        resetButton.addActionListener(e -> {
            setDefaultColors();
            mainPanel.invalidate();
        });
        
        colorsPanel.add(resetButton, constraints);
    }
    
    
    /**
     * Reload colors from app-settings.
     */
    public void updateColorPanels() {
        for (var colorKey : colorPanels.keySet()) {
            var color = appSettings.getColor(colorKey);
            var panels = colorPanels.get(colorKey);
            
            if (color == null || panels == null)
                continue;
            
            var colorTuple = deconstructJBColor(color);
            panels.a.setSelectedColor(colorTuple.first());
            panels.b.setSelectedColor(colorTuple.second());
        }
    }
    
    
    /**
     * Obtain all colors from the app-settings and update
     * the color-picker components.
     */
    void setDefaultColors() {
        for (var colorKey : colorPanels.keySet()) {
            var color = DefaultStyles.getDefaultColor(colorKey);
            var panels = colorPanels.get(colorKey);
            
            if (color == null || panels == null)
                continue;
            
            var colorTuple = deconstructJBColor(color);
            panels.a.setSelectedColor(colorTuple.first());
            panels.b.setSelectedColor(colorTuple.second());
        }
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
    private void addColorPanelComponent(JPanel target, String label, GridBagConstraints constraints, ColorKey colorKey, int row, boolean filledLocked) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.1;
        constraints.insets = emptyInsets;
        
        target.add(new JBLabel(label), constraints);
        
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
        constraints.weightx = 0.1;
        constraints.insets = cpInsets;
        
        target.add(panels.second(), constraints);
        
        constraints.gridx = 3;
        constraints.weightx = 0.1;
        constraints.insets = cpInsets;
        
        var state = appSettings.getCheckBoxState(colorKey);
        var cb = new Checkbox("");
        stateCheckBoxes.put(colorKey, cb);
        cb.setState(state);
        target.add(cb, constraints);
        
        if (filledLocked) {
            cb.setState(false);
            cb.setEnabled(false);
        }
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
     * Return the color panel data.
     *
     * @return The map containing the selected color data.
     */
    public Map<ColorKey, Tuple2<ColorPanel, ColorPanel>> getColorPanels() {
        return colorPanels;
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
     * Returns the state for the check-box relating to the given color-key.
     *
     * @param colorKey The color-key the check-box is associated.
     * @return The state of the check-box component.
     */
    public Boolean getSelectedState(ColorKey colorKey) {
        if (stateCheckBoxes.containsKey(colorKey)) {
            return stateCheckBoxes.get(colorKey).getState();
        } else {
            return DefaultStyles.getDefaultCheckBoxState(colorKey);
        }
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
