package com.bari;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

class CustomFileChooser extends JFileChooser {
    
    CustomFileChooser() {
        super();
    }

    @Override
    public void approveSelection() {
        File selectedFile = getSelectedFile();

        if (selectedFile.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "The file already exists. Do you want to overwrite it?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION);

            if (result != JOptionPane.YES_OPTION)
                return;
        }

        super.approveSelection();
    }
}

public class MenuBar extends JMenuBar {

    MainFrame parent;
    JMenu fileMenu;
    JMenu editMenu;
    JMenu helpMenu;
    AbstractAction deleteAction;
    AbstractAction undoAction;
    AbstractAction redoAction;
    JRadioButton lightThemeRadioButton;
    JRadioButton darkThemeRadioButton;

    MenuBar(MainFrame parent) {
        this.parent = parent;

        this.fileMenu = new JMenu("File");
        this.editMenu = new JMenu("Edit");
        this.helpMenu = new JMenu("Help");

        this.fileMenu.setFocusable(false);
        this.editMenu.setFocusable(false);
        this.helpMenu.setFocusable(false);

        this.fileMenu.add(new AbstractAction("New") {

            @Override
            public void actionPerformed(ActionEvent event) {
                parent.workspace.initializeProjectSource();
            }
        });
        this.fileMenu.add(new AbstractAction("Open") {

            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION) {
                    parent.workspace.openProjectSource(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        this.fileMenu.add(new AbstractAction("Save as..") {

            @Override
            public void actionPerformed(ActionEvent event) {
                performSaveAsAction();
            }
        });
        this.fileMenu.add(new AbstractAction("Save") {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (parent.workspace.projectSourcePath != null) {
                    parent.workspace.saveProjectSource(parent.workspace.projectSourcePath);
                } else {
                    performSaveAsAction();
                }
            }
        });
        this.fileMenu.addSeparator();
        this.fileMenu.add(new AbstractAction("Exit") {

            @Override
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });

        this.undoAction = new AbstractAction("Undo") {

            @Override
            public void actionPerformed(ActionEvent event) {
                parent.workspace.performUndo();
            }
        };
        this.undoAction.setEnabled(false);
        this.editMenu.add(this.undoAction);
        this.redoAction = new AbstractAction("Redo") {

            @Override
            public void actionPerformed(ActionEvent event) {
                parent.workspace.performRedo();
            }
        };
        this.redoAction.setEnabled(false);
        this.editMenu.add(this.redoAction);
        this.deleteAction = new AbstractAction("Delete") {

            @Override
            public void actionPerformed(ActionEvent event) {
                parent.workspace.deleteSelection();
            }
        };
        this.deleteAction.setEnabled(false);
        this.editMenu.add(deleteAction);
        this.editMenu.addSeparator();
        JMenu colorThemeMenu = new JMenu("Color Theme");
        lightThemeRadioButton = new JRadioButton(new AbstractAction("Light Theme ") {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (!lightThemeRadioButton.isSelected())
                    lightThemeRadioButton.setSelected(true);
                darkThemeRadioButton.setSelected(false);

                parent.workspace.applyTheme(WorkspaceTheme.LIGHT);
            }
        });
        darkThemeRadioButton = new JRadioButton(new AbstractAction("Dark Theme ") {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (!darkThemeRadioButton.isSelected())
                    darkThemeRadioButton.setSelected(true);
                lightThemeRadioButton.setSelected(false);

                parent.workspace.applyTheme(WorkspaceTheme.DARK);
            }
        });
        lightThemeRadioButton.setSelected(true);
        colorThemeMenu.add(lightThemeRadioButton);
        colorThemeMenu.add(darkThemeRadioButton);
        this.editMenu.add(colorThemeMenu);

        this.helpMenu.add(new AbstractAction("How to use?") {

            @Override
            public void actionPerformed(ActionEvent event) {
                // TODO: set the how to use dialog
            }
        });
        this.helpMenu.add(new AbstractAction("About") {

            @Override
            public void actionPerformed(ActionEvent event) {
                // TODO: set the about dialog
            }
        });

        this.add(this.fileMenu);
        this.add(this.editMenu);
        this.add(this.helpMenu);
    }

    void performSaveAsAction() {
        CustomFileChooser fileChooser = new CustomFileChooser();
        fileChooser.setSelectedFile(new File("Untitled"));
        int result = fileChooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        String selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!selectedFilePath.endsWith(".xml"))
            selectedFilePath = selectedFilePath.concat(".xml");
        parent.workspace.saveProjectSource(selectedFilePath);
    }
}
