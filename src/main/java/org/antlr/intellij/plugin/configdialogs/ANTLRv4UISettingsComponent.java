// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState.ColorKey;
import org.antlr.intellij.plugin.misc.FontManager;
import org.antlr.intellij.plugin.misc.FontManager.FontBundle;
import org.antlr.intellij.plugin.misc.Tuple2;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.antlr.intellij.plugin.ANTLRUtils.deconstructJBColor;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class ANTLRv4UISettingsComponent {
    // Configure Logger
    private static final Logger LOG = Logger.getInstance(ANTLRv4UISettingsComponent.class);
    
    
    // color panels stored by color-key.
    @Getter private final Map<ColorKey, Tuple2<ColorPanel, ColorPanel>> colorPanels
        = new LinkedHashMap<>();
    
    @Getter private final Map<ColorKey, Checkbox> stateCheckBoxes
        = new LinkedHashMap<>();
    
    private JPanel mainPanel;
    
    private final JCheckBox checkBoxAutoShow;
    private final JCheckBox checkBoxDebugMode;
    private final JCheckBox checkBoxFractionalMetrics;
    private final JComboBox<FontBundle> fontRegularComboBox;
    private final JComboBox<FontBundle> fontMonospacedComboBox;
    private final JBLabel fontRegularLabel;
    private final JBLabel fontMonospacedLabel;
    private final ANTLRv4UISettingsState appSettings;
    
    Insets emptyInsets = new JBInsets(0, 0, 0, 0);
    Insets cpInsets = JBUI.insetsLeft(8);
    
    
    /**
     * Creates a UI settings component.
     * Called automatically.
     */
    public ANTLRv4UISettingsComponent() {
        appSettings = ANTLRv4UISettingsState.getInstance();
        mainPanel = new JPanel(new BorderLayout());
        
        var commonSettingsPanel = new JPanel();
        commonSettingsPanel.setLayout(new BoxLayout(commonSettingsPanel, BoxLayout.Y_AXIS));
        commonSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Common Viewer Settings"));
        
        
        checkBoxAutoShow = new JBCheckBox("Automatically bring preview window in front when switching a grammar.");
        checkBoxDebugMode = new JBCheckBox("Enable DEBUG Mode for ANTLR I/O Console.");
        checkBoxFractionalMetrics = new JBCheckBox("Enable FractionalMetrics for the font-rendering.");
        // ...
        
        checkBoxAutoShow.setSelected(appSettings.isAutoShowAntlrTool());
        checkBoxDebugMode.setSelected(appSettings.isEnableDebugConsole());
        checkBoxFractionalMetrics.setSelected(appSettings.isUseFractionalMetrics());
        
        commonSettingsPanel.add(checkBoxAutoShow);
        commonSettingsPanel.add(checkBoxDebugMode);
        commonSettingsPanel.add(checkBoxFractionalMetrics);
        
        mainPanel = add(mainPanel, commonSettingsPanel);
        
        /*|--------------------------------------------------------------------------|*/
        
        var fontSettingsPanel = new JPanel(new GridBagLayout());
        fontSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Font Settings"));
        
        mainPanel = add(mainPanel, fontSettingsPanel);
        
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
        
        /* ----------------------------------------------------------------------- */
        
        fontRegularLabel = new JBLabel();
        fontRegularLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        fontMonospacedLabel = new JBLabel();
        fontMonospacedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        fontRegularComboBox = new ComboBox<>(FontManager.getBundles());
        fontRegularComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                var c = super.getListCellRendererComponent(list, value, index, sel, focus);
                var bundle = (FontBundle) value;
                setText(bundle.getLabel().getText());
                
                if (index >= 0) {
                    setFont(bundle.getFont().deriveFont(JBFont.regular().getSize() + 1.f));
                }
                
                return c;
            }
        });
        
        fontRegularComboBox.addActionListener(e -> {
            var bundle = (FontBundle) fontRegularComboBox.getSelectedItem();
            var font = bundle != null ? bundle.getFont() : FontManager.DEFAULT_FONT;
            setRegularFont(font);
        });
        
        setRegularFont(appSettings.getFontRegular());
        
        var comboBoxConstraints =
            new GridBagConstraints(
                0, 0, 1, 1, 0, 0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                emptyInsets,
                0, 0
            );
        
        comboBoxConstraints.gridx = 0;
        comboBoxConstraints.gridy = 1;
        comboBoxConstraints.weightx = 0.1;
        comboBoxConstraints.ipady = 0;
        comboBoxConstraints.gridwidth = 1;
        
        var fontLabel = new JBLabel("Regular");
        fontSettingsPanel.add(fontLabel, comboBoxConstraints);
        
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 0.7;
        comboBoxConstraints.gridy = 1;
        comboBoxConstraints.gridwidth = 2;
        fontSettingsPanel.add(fontRegularComboBox, comboBoxConstraints);
        fontRegularComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontRegularComboBox.setPreferredSize(
            new Dimension(
                FontManager.getMaximumWidth(),
                fontRegularComboBox.getPreferredSize().height)
        );
        
        comboBoxConstraints.gridx = 2;
        comboBoxConstraints.weightx = 0.9;
        comboBoxConstraints.gridy = 1;
        comboBoxConstraints.gridwidth = 1;
        
        fontSettingsPanel.add(fontRegularLabel, comboBoxConstraints);
        
        /* ----------------------------------------------------------------------- */
        
        fontMonospacedComboBox = new ComboBox<>(FontManager.getMonospacedBundles());
        fontMonospacedComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                var c = super.getListCellRendererComponent(list, value, index, sel, focus);
                var bundle = (FontBundle) value;
                setText(bundle.getLabel().getText());
                
                if (index >= 0) {
                    setFont(bundle.getFont().deriveFont(JBFont.regular().getSize() + 1.f));
                }
                
                return c;
            }
        });
        
        fontMonospacedComboBox.addActionListener(e -> {
            var bundle = (FontBundle) fontMonospacedComboBox.getSelectedItem();
            var font = bundle != null ? bundle.getFont() : FontManager.DEFAULT_FONT;
            setMonospacedFont(font);
        });
        
        setMonospacedFont(appSettings.getFontMonospaced());
        
        comboBoxConstraints =
            new GridBagConstraints(
                0, 0, 1, 1, 0, 0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                emptyInsets,
                0, 0
            );
        
        comboBoxConstraints.gridx = 0;
        comboBoxConstraints.gridy = 2;
        comboBoxConstraints.weightx = 0.1;
        comboBoxConstraints.ipady = 0;
        comboBoxConstraints.gridwidth = 1;
        
        var fontLabelMonospaced = new JBLabel("Monospaced");
        fontSettingsPanel.add(fontLabelMonospaced, comboBoxConstraints);
        
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 0.9;
        comboBoxConstraints.gridy = 2;
        comboBoxConstraints.gridwidth = 1;
        fontSettingsPanel.add(fontMonospacedComboBox, comboBoxConstraints);
        fontMonospacedComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontMonospacedComboBox.setPreferredSize(
            new Dimension(
                FontManager.getMaximumWidth(),
                fontMonospacedComboBox.getPreferredSize().height)
        );
        
        comboBoxConstraints.gridx = 2;
        comboBoxConstraints.weightx = 0.9;
        comboBoxConstraints.gridy = 2;
        comboBoxConstraints.gridwidth = 1;
        
        fontSettingsPanel.add(fontMonospacedLabel, comboBoxConstraints);
        
        mainPanel.invalidate();
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
        }
        
        return DefaultStyles.getDefaultCheckBoxState(colorKey);
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
    
    
    public JBFont getRegularFont() {
        var bundle = (FontBundle) fontRegularComboBox.getSelectedItem();
        
        if (bundle == null) {
            return FontManager.DEFAULT_FONT;
        }
        
        return bundle.getFont();
    }
    
    
    public void setRegularFont(final JBFont font) {
        var bundle = FontManager.getBundle(font.getFontName());
        
        fontRegularComboBox.setSelectedItem(bundle);
        fontRegularLabel.setFont(font);
        fontRegularLabel.setText(font.getFontName() + " / " + font.getFamily());
    }
    
    
    public JBFont getMonospacedFont() {
        var bundle = (FontBundle) fontMonospacedComboBox.getSelectedItem();
        
        if (bundle == null) {
            return FontManager.DEFAULT_FONT;
        }
        
        return bundle.getFont();
    }
    
    
    public void setMonospacedFont(final JBFont font) {
        var bundle = FontManager.getBundle(font.getFontName());
        
        fontMonospacedComboBox.setSelectedItem(bundle);
        fontMonospacedLabel.setFont(font);
        fontMonospacedLabel.setText(font.getFontName() + " / " + font.getFamily());
    }
    
    
    public boolean isAutoShow() {
        return checkBoxAutoShow.isSelected();
    }
    
    
    public void setAutoShow(boolean autoShow) {
        checkBoxAutoShow.setSelected(autoShow);
    }
    
    
    public boolean isDebugConsole() {
        return checkBoxDebugMode.isSelected();
    }
    
    
    public void setDebugConsole(boolean debugConsole) {
        checkBoxDebugMode.setSelected(debugConsole);
    }
    
    
    public boolean isFractionalMetrics() {
        return checkBoxFractionalMetrics.isSelected();
    }
    
    
    public void setFractionalMetrics(boolean fractionalMetrics) {
        checkBoxFractionalMetrics.setSelected(fractionalMetrics);
    }
    
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }
}
