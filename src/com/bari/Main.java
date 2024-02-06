package com.bari;

import java.awt.DefaultFocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;

class MainFrame extends JFrame {

    public GridBagLayout mainLayout;
    public GridBagConstraints gbc;
    public MenuBar menuBar;
    public ToolBar toolBar;
    public StatusBar statusBar;
    public JSplitPane centerView;
    public SidePanel sidePanel;
    public Workspace workspace;
    public String mainTitle;

    MainFrame(String title, int width, int height) {
        this.mainTitle = title;
        this.mainLayout = new GridBagLayout();
        this.gbc = new GridBagConstraints();

        this.setSize(width, height);
        this.setTitle(title);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLayout(mainLayout);
        this.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        this.initUi();

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog((JFrame) e.getSource(),
                        "Are you sure you want to exit?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    ((JFrame) e.getSource()).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        });
    }

    void initUi() {
        this.menuBar = new MenuBar(this);
        this.gbc.gridx = 0;
        this.gbc.gridy = 0;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.menuBar, this.gbc);

        this.toolBar = new ToolBar(this);
        this.gbc.gridx = 0;
        this.gbc.gridy = 1;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.toolBar, this.gbc);

        this.centerView = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.sidePanel = new SidePanel(this);
        this.centerView.setRightComponent(this.sidePanel);
        this.workspace = new Workspace(this);
        this.centerView.setLeftComponent(this.workspace);
        this.centerView.setResizeWeight(0.85);
        this.gbc.gridx = 0;
        this.gbc.gridy = 2;
        this.gbc.weightx = 1;
        this.gbc.weighty = 1;
        this.gbc.fill = GridBagConstraints.BOTH;
        this.add(this.centerView, this.gbc);

        this.statusBar = new StatusBar();
        this.gbc.gridx = 0;
        this.gbc.gridy = 3;
        this.gbc.weightx = 1;
        this.gbc.weighty = 0;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.statusBar, this.gbc);
    }
}

public class Main {

    public static void main(String[] args) {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        MainFrame mainFrame = new MainFrame("Network Routing Simulator", 900, 600);
        mainFrame.setVisible(true);
    }
}
