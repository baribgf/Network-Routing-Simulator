package com.bari;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.util.LinkedHashMap;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class SidePanel extends JPanel {

    MainFrame parent;
    GridBagLayout baseLayout;
    GridBagConstraints gbc;
    JTable propertiesTable;
    DefaultTableModel propertiesTableModel;
    JTable forwardingTable;
    DefaultTableModel forwardingTableModel;
    JTextPane infoArea;

    SidePanel(MainFrame parent) {
        this.parent = parent;
        this.baseLayout = new GridBagLayout();
        this.gbc = new GridBagConstraints();

        this.setLayout(baseLayout);

        this.propertiesTableModel = new DefaultTableModel(new String[] { "Property", "Value" }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return new Boolean[] { false, true }[column];
            }
        };
        this.propertiesTable = new JTable(this.propertiesTableModel);
        this.propertiesTable.setShowVerticalLines(true);
        // this.propertiesTable.setVisible(false);
        this.propertiesTableModel.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent event) {
                if (event.getType() == TableModelEvent.UPDATE) {
                    DefaultTableModel source = ((DefaultTableModel) event.getSource());
                    String key = (String) source.getValueAt(event.getFirstRow(), 0);
                    String value = (String) source.getValueAt(event.getFirstRow(), 1);

                    WSShape selectedShape = ((WSShape) parent.workspace.mainCanvas.selectedShapes.toArray()[0]);
                    switch (key) {
                        case "Name":
                            for (WSShape shape : parent.workspace.mainCanvas.shapes) {
                                if (shape.name.equals(value)) {
                                    JOptionPane.showMessageDialog(parent, "Invalid Name Entered !");
                                    return;
                                }
                            }
                            selectedShape.name = value;
                            break;

                        case "IP":
                            String IPAddressFormat = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
                            if (value.matches(IPAddressFormat)) {
                                selectedShape.ipAddress = NGNode.parseIP(value);
                            } else {
                                JOptionPane.showMessageDialog(parent, "Invalid IP Address Entered !");
                            }
                                
                            break;

                        case "AS Id":
                            try {
                                selectedShape.ASID = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(parent, "Invalid AS Id Entered !");
                            }
                            break;
                    }
                    parent.workspace.mainCanvas.repaint();
                }
            }
        });
        this.gbc.gridx = 0;
        this.gbc.gridy = 0;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0.5;
        this.gbc.fill = GridBagConstraints.BOTH;
        JScrollPane jsp1 = new JScrollPane(this.propertiesTable);
        jsp1.setPreferredSize(new Dimension(30, (int) jsp1.getMinimumSize().getHeight()));
        this.add(jsp1, this.gbc);

        this.forwardingTableModel = new DefaultTableModel(new String[] { "Src", "Dest", "Port" }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.forwardingTable = new JTable(this.forwardingTableModel);
        this.forwardingTable.setShowVerticalLines(true);
        this.gbc.gridx = 0;
        this.gbc.gridy = 1;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0.5;
        this.gbc.fill = GridBagConstraints.BOTH;
        JScrollPane jsp2 = new JScrollPane(this.forwardingTable);
        jsp2.setPreferredSize(new Dimension(30, (int) jsp2.getMinimumSize().getHeight()));
        this.add(jsp2, this.gbc);

        this.infoArea = new JTextPane();
        this.infoArea.setText("Empty");
        this.infoArea.setForeground(Color.DARK_GRAY);
        this.infoArea.setEditable(false);
        this.infoArea.setFocusable(false);
        this.gbc.gridx = 0;
        this.gbc.gridy = 2;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0.5;
        this.gbc.fill = GridBagConstraints.BOTH;
        this.add(this.infoArea, this.gbc);
    }

    public void clearProperties() {
        propertiesTableModel.setRowCount(0);
    }

    public void setProperties(LinkedHashMap<String, Object> props) {
        this.clearProperties();

        for (String key : props.keySet())
            this.propertiesTableModel.addRow(new Object[] { key, props.get(key) });

        this.propertiesTable.setVisible(true);
    }

    public void clearForwardingTable() {
        this.forwardingTableModel.setRowCount(0);
    }

    public void setForwardingTable(ForwardingTable forwardingTable) {
        this.clearForwardingTable();

        for (ForwardingTableEntry e : forwardingTable.getTableArray())
            this.forwardingTableModel.addRow(new Object[] { e.sourceAddress, e.destinationAddress, e.interfaceNumber });
    }
}
