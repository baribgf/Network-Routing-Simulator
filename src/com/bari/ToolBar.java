package com.bari;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JToggleButton;

public class ToolBar extends JMenuBar {

    MainFrame parent;
    FlowLayout baseLayout;
    JLabel placementLabel;
    JToggleButton routerPlacementButton;
    JToggleButton hostPlacementButton;
    JLabel protocolChooserLabel;
    JComboBox<String> protocolChooserBox;
    JToggleButton simulateButton;

    ToolBar(MainFrame parent) {
        this.parent = parent;
        this.baseLayout = new FlowLayout(FlowLayout.LEFT);
        this.setLayout(baseLayout);

        this.placementLabel = new JLabel("Place: ");
        this.add(placementLabel);

        this.routerPlacementButton = new JToggleButton("Router");
        this.routerPlacementButton.setFocusable(false);
        this.routerPlacementButton.addActionListener(event -> {
            this.hostPlacementButton.setSelected(false);
            if (routerPlacementButton.isSelected()) {
                parent.workspace.setMode(WorkspaceMode.DRAWING);
                parent.workspace.shapeType = ShapeType.ROUTER;
                parent.workspace.toggledButton = this.routerPlacementButton;
            } else {
                parent.workspace.setMode(WorkspaceMode.NONE);
                parent.workspace.shapeType = ShapeType.NONE;
                parent.workspace.toggledButton = null;
            }
        });
        this.add(routerPlacementButton);

        this.hostPlacementButton = new JToggleButton("Host");
        this.hostPlacementButton.setFocusable(false);
        this.hostPlacementButton.addActionListener(event -> {
            this.routerPlacementButton.setSelected(false);
            if (this.hostPlacementButton.isSelected()) {
                parent.workspace.setMode(WorkspaceMode.DRAWING);
                parent.workspace.shapeType = ShapeType.HOST;
                parent.workspace.toggledButton = this.hostPlacementButton;
            } else {
                parent.workspace.setMode(WorkspaceMode.NONE);
                parent.workspace.shapeType = ShapeType.NONE;
                parent.workspace.toggledButton = null;
            }
        });
        this.add(hostPlacementButton);

        this.protocolChooserLabel = new JLabel("Choose Protocol: ");
        this.protocolChooserLabel.setSize(1, this.protocolChooserLabel.getHeight());
        this.add(protocolChooserLabel);

        this.protocolChooserBox = new JComboBox<>();
        this.protocolChooserBox.addItem("");
        this.protocolChooserBox.addItem("Open Shortest Path First (OSPF)");
        this.protocolChooserBox.addItem("Border Gateway Protocol (BGP)");
        this.protocolChooserBox
                .setPreferredSize(new Dimension(190, (int) this.protocolChooserBox.getMinimumSize().getHeight()));
        this.protocolChooserBox.setFocusable(false);
        this.add(protocolChooserBox);

        this.simulateButton = new JToggleButton("Simulate");
        this.simulateButton.setFocusable(false);
        this.simulateButton.addActionListener(event -> {
            if (simulateButton.isSelected()) {
                int index = protocolChooserBox.getSelectedIndex();
                SimulationType simType;
                if (index == 1) {
                    simType = SimulationType.OSPF;
                } else if (index == 2) {
                    simType = SimulationType.BGP;
                } else {
                    return;
                }
                parent.workspace.currentSimulationType = simType;
                parent.workspace.startSimulation(simType);
            } else {
                parent.workspace.stopSimulation();
            }
        });
        this.add(this.simulateButton);
    }
}
