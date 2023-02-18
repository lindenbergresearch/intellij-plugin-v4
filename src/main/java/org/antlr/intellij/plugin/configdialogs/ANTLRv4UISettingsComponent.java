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

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class ANTLRv4UISettingsComponent {
    
    private final Map<ColorKey, Tuple2<ColorPanel, ColorPanel>> colorPanels
        = new LinkedHashMap<>();
    
    
    private JPanel mainPanel;
    
    public ColorPanel chooseBackground;
    public ColorPanel chooseTextColor;
    public ColorPanel chooseConnectorColor;
    public ColorPanel chooseRootColor;
    
    private JCheckBox scrollToFirst;
    private JCheckBox scrollToLast;
    private ANTLRv4UISettingsState appSettings;
    
    Insets emptyInsets = new JBInsets(0);
    Insets cpInsets = JBUI.insetsLeft(18);
    
    
    public ANTLRv4UISettingsComponent() {
        appSettings = ANTLRv4UISettingsState.getInstance();
        mainPanel = new JPanel(new BorderLayout());
        
        
        var commonSettingsPanel = new JPanel();
        commonSettingsPanel.setLayout(new BoxLayout(commonSettingsPanel, BoxLayout.Y_AXIS));
        commonSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Parse-Tree Color Settings"));
        
        
        scrollToFirst = new JCheckBox("Scroll to first?");
        scrollToLast = new JCheckBox("Scroll to last?");
        
        
        commonSettingsPanel.add(scrollToFirst);
        commonSettingsPanel.add(scrollToLast);
        
        
        mainPanel = add(mainPanel, commonSettingsPanel);
        
        
        var colorsPanel = new JPanel(new GridBagLayout());
        colorsPanel.setBorder(IdeBorderFactory.createTitledBorder("Parse-Tree Color Settings"));
        
        mainPanel = add(mainPanel, colorsPanel);
        
        
        var constraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, emptyInsets, 0, 0);
        int i = 0;
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
        
        /*|--------------------------------------------------------------------------|*/
//
//        colorsPanel.add(new JLabel("Parse-Tree-Viewer Background"), constraints);
//
//        chooseBackground = new ColorPanel();
//        chooseBackground.setSelectedColor(appSettings.getColor(ColorKey.VIEWER_BACKGROUND));
//
//        constraints.gridx = 1;
//        constraints.gridy = 0;
//        constraints.weightx = 1;
//        constraints.insets = cpInsets;
//        colorsPanel.add(chooseBackground, constraints);
//
//        /*|--------------------------------------------------------------------------|*/
//
//        constraints.gridx = 0;
//        constraints.gridy = 1;
//        constraints.weightx = 0;
//        constraints.insets = emptyInsets;
//        colorsPanel.add(new JLabel("Text and Labels"), constraints);
//
//        constraints.gridx = 1;
//        constraints.gridy = 1;
//        constraints.weightx = 1;
//        constraints.insets = cpInsets;
//
//        chooseTextColor = new ColorPanel();
//        chooseTextColor.setSelectedColor(appSettings.getColor(ColorKey.TEXT_COLOR));
//        colorsPanel.add(chooseTextColor, constraints);
//
//        /*|--------------------------------------------------------------------------|*/
//
//        constraints.gridx = 0;
//        constraints.gridy = 2;
//        constraints.weightx = 0;
//        constraints.insets = emptyInsets;
//        colorsPanel.add(new JLabel("Connectors"), constraints);
//
//        constraints.gridx = 1;
//        constraints.gridy = 2;
//        constraints.weightx = 1;
//        constraints.insets = cpInsets;
//
//        chooseConnectorColor = new ColorPanel();
//        chooseConnectorColor.setSelectedColor(appSettings.getColor(ColorKey.CONNECTOR_COLOR));
//        colorsPanel.add(chooseConnectorColor, constraints);
//
//        /*|--------------------------------------------------------------------------|*/
//
//        constraints.gridx = 0;
//        constraints.gridy = 3;
//        constraints.weightx = 0;
//        constraints.insets = emptyInsets;
//        colorsPanel.add(new JLabel("Root node"), constraints);
//
//        constraints.gridx = 1;
//        constraints.gridy = 3;
//        constraints.weightx = 1;
//        constraints.insets = cpInsets;
//
//        chooseRootColor = new ColorPanel();
//        chooseRootColor.setSelectedColor(appSettings.getColor(ColorKey.ROOT_NODE_COLOR));
//        colorsPanel.add(chooseRootColor, constraints);
//
//        /*|--------------------------------------------------------------------------|*/
//
//        var resetButton = new JButton("Reset");
//
//        constraints.gridx = 0;
//        constraints.gridy = 4;
//        constraints.weightx = 0;
//        constraints.insets = new JBInsets(0, -2, 0, 0);
//        colorsPanel.add(resetButton, constraints);

//        var otherPanel = new JPanel();
//        otherPanel.setLayout(new BoxLayout(otherPanel, BoxLayout.Y_AXIS));
//        otherPanel.setBorder(IdeBorderFactory.createTitledBorder("Other Settings Just for You"));
//
//
//        otherPanel.add(new JLabel("sdfgsdfgsdfg"));
//        otherPanel.add(new JLabel("sdfgsdfgsdfg"));
//        otherPanel.add(new JLabel("sdfgsdfgsdfg"));
//        otherPanel.add(new JLabel("sdfgsdfgsdfg"));
//
//        mainPanel = add(mainPanel, otherPanel);
    
    }
    
    
    /**
     * @param target
     * @param label
     * @param constraints
     * @param colorKey
     * @param row
     */
    private void addColorPanelComponent(JPanel target, String label, GridBagConstraints constraints, ColorKey colorKey, int row) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        constraints.insets = emptyInsets;
        
        target.add(new JLabel(label), constraints);
        
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.insets = cpInsets;
        
        var color = appSettings.getColor(colorKey);
        
        var panels = getOrCreate(colorKey);
        panels.a.setSelectedColor(color);
        panels.b.setSelectedColor(color.getDarkVariant());
        
        target.add(panels.a, constraints);
        
        constraints.gridx = 2;
        constraints.insets = emptyInsets;
        
        target.add(panels.b, constraints);
    }
    
    
    /**
     * @param panel
     * @param component
     * @return
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
        System.out.println("getOrCreate(" + colorKey + ')');
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
     * @param colorKey
     * @return
     */
    public JBColor getSelectedColors(ColorKey colorKey) {
        System.out.println("getSelectedColors(" + colorKey + ')');
        var tuple = colorPanels.get(colorKey);
        
        if (tuple == null || tuple.isNull()) return
            DefaultStyles.getDefaultColor(colorKey);
        
        return new JBColor(
            Objects.requireNonNull(tuple.a.getSelectedColor()),
            Objects.requireNonNull(tuple.b.getSelectedColor())
        );
    }
    
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    
    public JComponent getPreferredFocusedComponent() {
        return chooseBackground;
    }
    
}
