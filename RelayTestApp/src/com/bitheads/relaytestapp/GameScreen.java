package com.bitheads.relaytestapp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.util.Timer;
import java.util.TimerTask;

class GameScreen extends Screen
{
    private BufferedImage _cursors[];
    private Timer _refreshTimer = new Timer();

    class PlayArea extends JPanel
    {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            State state = App.getInstance().state;

            // Shockwaves
            for (int i = 0; i < state.shockwaves.size(); ++i)
            {
                Shockwave shockwave = state.shockwaves.get(i);
                shockwave.time++;
                if (shockwave.time >= 30) // 1sec life
                {
                    state.shockwaves.remove(i);
                    --i;
                    continue;
                }
                int size = shockwave.time * 128 / 30;
                g.setColor(shockwave.color);
                g.drawArc(shockwave.pos.x - size / 2, shockwave.pos.y - size / 2, size, size, 0, 360);
            }

            // Players' arrows
            for (int i = 0; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                if (member.pos != null)
                {
                    g.drawImage(_cursors[member.colorIndex], member.pos.x, member.pos.y, null);
                }
            }
        }
    }

    public GameScreen()
    {
        try
        {
            _cursors = new BufferedImage[] {
                ImageIO.read(new File("bin/assets/arrow0.png")),
                ImageIO.read(new File("bin/assets/arrow1.png")),
                ImageIO.read(new File("bin/assets/arrow2.png")),
                ImageIO.read(new File("bin/assets/arrow3.png")),
                ImageIO.read(new File("bin/assets/arrow4.png")),
                ImageIO.read(new File("bin/assets/arrow5.png")),
                ImageIO.read(new File("bin/assets/arrow6.png")),
                ImageIO.read(new File("bin/assets/arrow7.png"))
            };
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        panel = new JPanel();
        panel.setLayout(null);

        refreshUI();
    }

    @Override
    public void onStateChanged(State state)
    {
        refreshUI();
    }

    void refreshUI()
    {
        panel.removeAll();
        State state = App.getInstance().state;

        JFrame frame = App.getInstance().frame;
        Dimension screenRes = frame.getPreferredSize();

        // Tittle
        {
            JLabel lblTitle = new JLabel("Move mouse around and Click", SwingConstants.CENTER);
            lblTitle.setSize(screenRes.width, screenRes.height / 2 - 350);
            lblTitle.setLocation(0, 40);
            Font font = lblTitle.getFont();
            lblTitle.setFont(new Font(font.getName(), Font.PLAIN, 32));
            panel.add(lblTitle);
        }

        // Options
        {
            JLabel lblPlayerMasks = new JLabel("Player Mask (For shockwaves)");
            lblPlayerMasks.setSize(200, 16);
            lblPlayerMasks.setLocation(8, 8);
            panel.add(lblPlayerMasks);

            int i = 0;
            for (; i < state.lobby.members.size(); ++i)
            {
                User member = state.lobby.members.get(i);
                JCheckBox chkPlayer = new JCheckBox(member.name, member.allowSendTo);
                chkPlayer.setSize(200, 16);
                chkPlayer.setLocation(8, 8 + 32 + i * 16);
                ChangeListener orderedChangeListener = new ChangeListener()
                {
                    public void stateChanged(ChangeEvent changeEvent)
                    {
                        AbstractButton abstractButton = (AbstractButton)changeEvent.getSource();
                        ButtonModel buttonModel = abstractButton.getModel();
                        member.allowSendTo = buttonModel.isSelected();
                    }
                };
                chkPlayer.addChangeListener(orderedChangeListener);
                panel.add(chkPlayer);
            }

            JLabel lblRelayOptions = new JLabel("Relay Options (For cursor position)");
            lblRelayOptions.setSize(200, 16);
            lblRelayOptions.setLocation(8, 8 + 32 + i * 16 + 32);
            panel.add(lblRelayOptions);
        
            JCheckBox chkReliable = new JCheckBox("Reliable", state.reliable);
            chkReliable.setSize(200, 16);
            chkReliable.setLocation(8, 8 + 32 + i * 16 + 32 + 32);
            ChangeListener reliableChangeListener = new ChangeListener()
            {
                public void stateChanged(ChangeEvent changeEvent)
                {
                    AbstractButton abstractButton = (AbstractButton)changeEvent.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    state.reliable = buttonModel.isSelected();
                }
            };
            chkReliable.addChangeListener(reliableChangeListener);
            panel.add(chkReliable);

        
            JCheckBox chkOrdered = new JCheckBox("Ordered", state.ordered);
            chkOrdered.setSize(200, 16);
            chkOrdered.setLocation(8, 8 + 32 + i * 16 + 32 + 32 + 16);
            ChangeListener orderedChangeListener = new ChangeListener()
            {
                public void stateChanged(ChangeEvent changeEvent)
                {
                    AbstractButton abstractButton = (AbstractButton)changeEvent.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    state.ordered = buttonModel.isSelected();
                }
            };
            chkReliable.addChangeListener(orderedChangeListener);
            panel.add(chkOrdered);
        }

        // Play area
        {
            JPanel playArea = new PlayArea();
            playArea.setSize(800, 600);
            playArea.setLocation(screenRes.width - 808, screenRes.height / 2 - 300);
            playArea.setBackground(Color.decode("#282c34"));
            panel.add(playArea);

            playArea.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseMoved(MouseEvent e)
                {
                    App.getInstance().onPlayerMove(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e)
                {
                    App.getInstance().onPlayerMove(e.getX(), e.getY());
                }
            });

            playArea.addMouseListener(new MouseListener()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    App.getInstance().onPlayerShockwave(e.getX(), e.getY());
                }

                @Override
                public void mouseClicked(MouseEvent e) {}
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
            });

            _refreshTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    playArea.repaint();
                }
            }, 0, 1000 / 60);
        }

        // Leave button
        {
            JButton btnLeave = new JButton("Leave");
            btnLeave.setSize(200, 30);
            btnLeave.setLocation(screenRes.width / 2 - 100, screenRes.height / 2 + 310);
            panel.add(btnLeave);
            btnLeave.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    App.getInstance().onGameScreenClose();
                }
            });
        }
    }
}
