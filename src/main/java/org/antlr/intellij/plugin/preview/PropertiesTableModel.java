package org.antlr.intellij.plugin.preview;

import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Table model for displaying 2 column mode properties.
 */
class PropertiesTableModelModel implements TableModel {
    
    Map<String, Object> properties;
    
    String[] columnNames;
    
    
    /**
     * Create table model and setup column count and names.
     *
     * @param columnNames Header column names.
     */
    public PropertiesTableModelModel(String... columnNames) {
        this.columnNames = columnNames;
        properties = new HashMap<String, Object>();
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
        if (row == 0) {
            return getColumnName(col);
        }
        
        if (row > 0 && col < getColumnCount()) {
            String key = (String) properties.keySet().toArray()[row];
            // return key if first column in properties table
            if (col == 0) return key;
            // else return the associated value
            return properties.get(key);
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
    
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    
    public String[] getColumnNames() {
        return columnNames;
    }
}
