package org.antlr.intellij.plugin.preview;

import org.antlr.v4.runtime.misc.Pair;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for displaying 2 column mode properties.
 */
class PropertiesTableModelModel implements TableModel {
    public static int PROPERTIES_NAME_COLUMN = 0;
    public static int PROPERTIES_VALUE_COLUMN = 1;
    
    
    private final List<Pair<String, Object>> properties;
    private final String[] columnNames;
    
    
    /**
     * Create table model and setup column count and names.
     *
     * @param columnNames Header column names.
     */
    public PropertiesTableModelModel(String... columnNames) {
        this.columnNames = columnNames;
        properties = new ArrayList<>();
    }
    
    
    @Override
    public int getRowCount() {
        return properties.size();
    }
    
    
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    
    @Nls
    @Override
    public String getColumnName(int i) {
        return i >= columnNames.length ?
            "???" :
            columnNames[i];
    }
    
    
    @Override
    public Class<?> getColumnClass(int i) {
        return getValueAt(0, i).getClass();
    }
    
    
    @Override
    public boolean isCellEditable(int i, int i1) {
        return false;
    }
    
    
    @Override
    public Object getValueAt(int row, int col) {
        if (row >= 0 && col < getColumnCount()) {
            var entry =
                properties.get(row);
            
            if (col == PROPERTIES_NAME_COLUMN) {
                return entry.a;
            }
            
            if (col == PROPERTIES_VALUE_COLUMN) {
                return entry.b;
            }
        }
        
        return null;
    }
    
    
    @Override
    public void setValueAt(Object o, int i, int i1) {
    }
    
    
    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {
    
    }
    
    
    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {
    
    }
    
    
    public List<Pair<String, Object>> getProperties() {
        return properties;
    }
    
    
    public String[] getColumnNames() {
        return columnNames;
    }
    
    
    public void clear() {
        properties.clear();
    }
}
