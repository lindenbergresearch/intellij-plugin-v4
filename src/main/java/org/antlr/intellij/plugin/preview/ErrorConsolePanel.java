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
    
    private JBList<String> list;
    private JBScrollPane scrollPane;
    private DefaultListModel<String> listModel;
    
    private static ErrorConsolePanel instance;
    
    
    /**
     * Creates a message pane with the given layout and border.
     *
     * @param layout Layout.
     * @param border Border.
     */
    public ErrorConsolePanel(LayoutManager layout, Border border) {
        super(layout, true);
        
        list = new JBList<>();
        
        list.setEmptyText("No errors found while parsing.");
        listModel = new DefaultListModel<>();
        
        list.setModel(listModel);
        
        
        scrollPane = new JBScrollPane(
            list,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        
        add(scrollPane);
        setBorder(border);
        
        list.setFont(DefaultStyles.MONOSPACE_FONT);
        list.setBackground(DefaultStyles.getConsoleBackground());
        list.setForeground(JBColor.RED);
        
        setInstance(this);
    }
    
    
    public static ErrorConsolePanel getInstance() {
        return instance;
    }
    
    
    public static void setInstance(ErrorConsolePanel instance) {
        ErrorConsolePanel.instance = instance;
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
