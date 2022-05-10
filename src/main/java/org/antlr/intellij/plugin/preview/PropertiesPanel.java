package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Panel for displaying 2 column mode
 * properties in the form: key - value
 */
public class PropertiesPanel extends JPanel {
    private JBTable propertiesTable;
    private PropertiesTableModelModel propertiesTableModelModel;
    private JBScrollPane scrollPane;
    private JLabel caption;
    
    
    public PropertiesPanel(LayoutManager layout, Border border) {
        super(layout, true);
        setBorder(border);
        
        propertiesTableModelModel =
            new PropertiesTableModelModel("Property", "Value");
        
        propertiesTable = new JBTable(propertiesTableModelModel);
        propertiesTable.setFillsViewportHeight(true);
        
        scrollPane = new JBScrollPane(
            propertiesTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        
        caption = new JLabel(
            "Object Explorer",
            AllIcons.CodeStyle.AddNewSectionRule,
            SwingConstants.CENTER
        );
        
        add(caption, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        
    }
    
    
    public JBTable getPropertiesTable() {
        return propertiesTable;
    }
    
    
    public void setPropertiesTable(JBTable propertiesTable) {
        this.propertiesTable = propertiesTable;
    }
}
