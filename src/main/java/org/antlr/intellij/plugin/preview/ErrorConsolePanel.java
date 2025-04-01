package org.antlr.intellij.plugin.preview;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.preview.ui.DefaultStyles;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Small custom list box for listing error messages from parser etc.
 */
public class ErrorConsolePanel extends JPanel {
    
    private final DefaultListModel<String> listModel;
    
    
    /**
     * Creates a message pane with the given layout and border.
     *
     * @param layout Layout.
     * @param border Border.
     */
    public ErrorConsolePanel(LayoutManager layout, Border border) {
        super(layout, true);
        
        var list = new JBList<String>();
        
        list.setEmptyText("No errors found while parsing.");
        listModel = new DefaultListModel<>();
        
        list.setModel(listModel);
        
        
        var scrollPane = new JBScrollPane(
            list,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        
        add(scrollPane);
        setBorder(border);
        
        list.setFont(DefaultStyles.BaseFontConsole.getFont());
        list.setBackground(DefaultStyles.getConsoleBackground());
        list.setForeground(JBColor.RED);
    }
    
    
    /**
     * Clear all data from list.
     */
    public void clear() {
        listModel.clear();
        invalidate();
    }
    
    
    /**
     * Add a message to the list.
     *
     * @param message Message as string.
     */
    public void add(String message) {
        listModel.addElement(message);
        invalidate();
    }
}
