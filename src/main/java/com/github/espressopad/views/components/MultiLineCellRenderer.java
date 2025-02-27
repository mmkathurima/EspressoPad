package com.github.espressopad.views.components;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {
    public MultiLineCellRenderer() {
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
    ) {
        this.setText(value.toString());
        if (isSelected) {
            this.setForeground(table.getSelectionForeground());
            this.setBackground(table.getSelectionBackground());
        } else {
            this.setForeground(table.getForeground());
            this.setBackground(table.getBackground());
        }
        this.setSize(table.getColumnModel().getColumn(column).getWidth(), this.getPreferredSize().height);
        if (table.getRowHeight(row) != this.getPreferredSize().height)
            table.setRowHeight(row, this.getPreferredSize().height);
        return this;
    }
}
