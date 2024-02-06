package com.bari;

import javax.swing.JLabel;
import javax.swing.JMenuBar;

public class StatusBar extends JMenuBar {
    JLabel defaultLabel;
    StatusBar() {
        this.defaultLabel = new JLabel("Welcome.");
        this.add(defaultLabel);
    }
}
