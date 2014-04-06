/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Cosmin
 */
public class KDCView extends JFrame {

    private JButton startKDCButton;
    private JPanel guiPanel;
    private KDC server;

    public KDCView() {
        guiPanel = new JPanel(new BorderLayout(20, 20));
        startKDCButton = new JButton("Start KDC");
        startKDCButton.setFont(new java.awt.Font("Tahoma", 1, 22));
        guiPanel.add(startKDCButton, BorderLayout.CENTER);
        add(guiPanel);

        startKDCButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                server = null;
                try {
                    server = new KDC();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    dispose();
                    if (server.getOut() != null) {
                        server.getOut().close();
                    }
                    if (server.getIn() != null) {
                        try {
                            server.getIn().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (server.getClientSocket() != null) {
                        try {
                            server.getClientSocket().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
