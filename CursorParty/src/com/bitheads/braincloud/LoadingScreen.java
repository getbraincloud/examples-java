package com.bitheads.braincloud;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class LoadingScreen extends Screen
{
    // Swing UI stuff
    JLabel _lblConnecting;

    public LoadingScreen(String text)
    {
        // Create the Swing stuff
        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        panel = new JPanel();
        panel.setLayout(null);
        
        _lblConnecting = new JLabel(text, SwingConstants.CENTER);
        _lblConnecting.setSize(screenRes.width, 20);
        _lblConnecting.setLocation(0, screenRes.height / 2 - 10);
        panel.add(_lblConnecting);
    }
}
