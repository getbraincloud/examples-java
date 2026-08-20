package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// Reusable chat widget, shared by global chat and this-lobby (signal) chat —
// mirrors cpp's shared drawGlobalChatContent()/drawLobbySignalChat() split.
//
// The message log and input controls are created ONCE and never rebuilt via
// removeAll(): every other screen in this app "wipes and rebuilds" its whole
// panel on refresh, but that idiom would destroy in-progress typed text and
// steal input focus here since chat updates can arrive several times a
// second (an incoming message must not interrupt someone mid-sentence).
public class ChatPanel extends JPanel
{
    public interface Source
    {
        ArrayList<ChatMessage> getMessages();
        void sendMessage(String text);
    }

    private final Source _source;
    private final JTextArea _log;
    private final JTextField _input;
    private int _lastRenderedCount = -1;

    public ChatPanel(Source source, int width, int height)
    {
        _source = source;
        setLayout(null);
        setBackground(Colors.BG_COLOR);
        setPreferredSize(new java.awt.Dimension(width, height));

        _log = new JTextArea();
        _log.setEditable(false);
        _log.setLineWrap(true);
        _log.setWrapStyleWord(true);
        _log.setBackground(new Color(0x20, 0x24, 0x2b));
        _log.setForeground(Colors.TEXT_COLOR);
        _log.setFont(new Font("SansSerif", Font.PLAIN, 12));

        int logHeight = height - 32;
        JScrollPane scroll = new JScrollPane(_log);
        scroll.setBounds(0, 0, width, logHeight);
        add(scroll);

        _input = new JTextField();
        _input.setBounds(0, logHeight + 6, width - 70, 26);
        add(_input);

        JButton btnSend = new JButton("Send");
        btnSend.setBounds(width - 64, logHeight + 6, 64, 26);
        add(btnSend);

        ActionListener sendAction = e -> {
            String text = _input.getText().trim();
            if (!text.isEmpty())
            {
                _source.sendMessage(text);
                _input.setText("");
                refresh();
            }
        };
        btnSend.addActionListener(sendAction);
        _input.addActionListener(sendAction);

        refresh();
    }

    // Re-renders the message log only. Safe to call often (RTT push, timer tick).
    public void refresh()
    {
        ArrayList<ChatMessage> messages = _source.getMessages();
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages)
            sb.append(m.fromName).append(": ").append(m.text).append('\n');

        _log.setText(sb.toString());
        if (messages.size() != _lastRenderedCount)
        {
            _lastRenderedCount = messages.size();
            SwingUtilities.invokeLater(() -> _log.setCaretPosition(_log.getDocument().getLength()));
        }
    }
}
