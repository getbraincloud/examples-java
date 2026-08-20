package com.bitheads.cursorparty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;
import com.bitheads.braincloud.services.SocialLeaderboardService;

import org.json.JSONArray;
import org.json.JSONObject;

// Reusable leaderboard viewer — mirrors cpp's leaderboardPanel.cpp. Board type
// (points/coverage) x period (lifetime/quarterly) toggle, top-5 + "you" row.
// Fetch is session-cached per (board,period) combo so re-opening the tab
// doesn't re-fetch.
public class LeaderboardPanel extends JPanel
{
    private int _boardType = 0; // 0 = points (Most Opponents Beaten), 1 = coverage (Highest Coverage %)
    private int _period = 0;    // 0 = Lifetime, 1 = Quarterly
    private int _fetchedKey = -1;

    private ArrayList<LeaderboardEntry> _top = new ArrayList<>();
    private LeaderboardEntry _self = null;

    private final int _width;
    private JButton _btnPoints, _btnCoverage, _btnLifetime, _btnQuarterly;
    private JPanel _listPanel;

    private static final Color ACTIVE_BG = new Color(0x40, 0x8C, 0x59);

    public LeaderboardPanel(int width, int height)
    {
        _width = width;
        setLayout(null);
        setBackground(Colors.BG_COLOR);
        setPreferredSize(new Dimension(width, height));

        _btnPoints = new JButton("Most Opponents Beaten");
        _btnPoints.setBounds(0, 0, width, 26);
        add(_btnPoints);

        _btnCoverage = new JButton("Highest Coverage %");
        _btnCoverage.setBounds(0, 28, width, 26);
        add(_btnCoverage);

        _btnLifetime = new JButton("Lifetime");
        _btnLifetime.setBounds(0, 60, width / 2 - 2, 24);
        add(_btnLifetime);

        _btnQuarterly = new JButton("Quarterly");
        _btnQuarterly.setBounds(width / 2 + 2, 60, width / 2 - 2, 24);
        add(_btnQuarterly);

        _listPanel = new JPanel();
        _listPanel.setLayout(null);
        _listPanel.setBounds(0, 92, width, Math.max(0, height - 92));
        _listPanel.setBackground(Colors.BG_COLOR);
        add(_listPanel);

        ActionListener boardListener = e -> {
            _boardType = (e.getSource() == _btnCoverage) ? 1 : 0;
            updateToggleStyles();
            fetchIfNeeded();
        };
        _btnPoints.addActionListener(boardListener);
        _btnCoverage.addActionListener(boardListener);

        ActionListener periodListener = e -> {
            _period = (e.getSource() == _btnQuarterly) ? 1 : 0;
            updateToggleStyles();
            fetchIfNeeded();
        };
        _btnLifetime.addActionListener(periodListener);
        _btnQuarterly.addActionListener(periodListener);

        updateToggleStyles();
        fetchIfNeeded();
    }

    private void updateToggleStyles()
    {
        _btnPoints.setBackground(_boardType == 0 ? ACTIVE_BG : null);
        _btnCoverage.setBackground(_boardType == 1 ? ACTIVE_BG : null);
        _btnLifetime.setBackground(_period == 0 ? ACTIVE_BG : null);
        _btnQuarterly.setBackground(_period == 1 ? ACTIVE_BG : null);
    }

    private String currentLeaderboardId()
    {
        State state = App.getInstance().state;
        boolean coverage = (_boardType == 1);
        boolean quarterly = (_period == 1);
        if (coverage)
            return quarterly ? state.coverageLeaderboardIdQuarterly : state.coverageLeaderboardId;
        return quarterly ? state.pointsLeaderboardIdQuarterly : state.pointsLeaderboardId;
    }

    private static LeaderboardEntry parseEntry(JSONObject entry)
    {
        LeaderboardEntry row = new LeaderboardEntry();
        row.score = entry.optLong("score", 0);
        row.rank = entry.optInt("rank", 0);
        String name = entry.optJSONObject("data") != null
                ? entry.getJSONObject("data").optString("name", "") : "";
        row.name = name.isEmpty() ? "Player" : name;
        return row;
    }

    // Fetches top-5 + the local player's own rank for the currently-selected
    // board/period combo. Guarded by _fetchedKey so switching tabs back and
    // forth re-shows cached results rather than re-fetching every time.
    private void fetchIfNeeded()
    {
        int key = _boardType * 2 + _period;
        if (_fetchedKey == key)
        {
            renderList();
            return;
        }
        _fetchedKey = key;
        final String leaderboardId = currentLeaderboardId();
        final int requestKey = key;

        App.getInstance()._bcWrapper.getSocialLeaderboardService().getGlobalLeaderboardPage(
                leaderboardId, SocialLeaderboardService.SortOrder.HIGH_TO_LOW, 0, 4,
                new IServerCallback()
                {
                    @Override
                    public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject result)
                    {
                        if (_fetchedKey != requestKey) return; // stale — user switched tabs since this fired
                        ArrayList<LeaderboardEntry> top = new ArrayList<>();
                        JSONArray arr = result.getJSONObject("data").getJSONArray("leaderboard");
                        for (int i = 0; i < arr.length(); i++)
                            top.add(parseEntry(arr.getJSONObject(i)));
                        _top = top;
                        SwingUtilities.invokeLater(LeaderboardPanel.this::renderList);
                    }

                    @Override
                    public void serverError(ServiceName sn, ServiceOperation so, int statusCode, int reasonCode, String jsonError)
                    {
                        if (_fetchedKey != requestKey) return;
                        _top = new ArrayList<>();
                        SwingUtilities.invokeLater(LeaderboardPanel.this::renderList);
                    }
                });

        // beforeCount=0/afterCount=0 on GetGlobalLeaderboardView returns just the
        // current player's own entry.
        App.getInstance()._bcWrapper.getSocialLeaderboardService().getGlobalLeaderboardView(
                leaderboardId, SocialLeaderboardService.SortOrder.HIGH_TO_LOW, 0, 0,
                new IServerCallback()
                {
                    @Override
                    public void serverCallback(ServiceName sn, ServiceOperation so, JSONObject result)
                    {
                        if (_fetchedKey != requestKey) return;
                        JSONArray arr = result.getJSONObject("data").getJSONArray("leaderboard");
                        _self = arr.length() > 0 ? parseEntry(arr.getJSONObject(0)) : null;
                        SwingUtilities.invokeLater(LeaderboardPanel.this::renderList);
                    }

                    @Override
                    public void serverError(ServiceName sn, ServiceOperation so, int statusCode, int reasonCode, String jsonError)
                    {
                        if (_fetchedKey != requestKey) return;
                        _self = null;
                        SwingUtilities.invokeLater(LeaderboardPanel.this::renderList);
                    }
                });

        renderList(); // show "Loading..." immediately
    }

    // Coverage board's raw score is basis points (posted as coveragePct*100 since
    // brainCloud leaderboard scores are int64 — no float score type); divide back
    // down for display. Points board's raw score is already the real value.
    private String formatBoardScore(long v)
    {
        if (_boardType == 1)
            return String.format("%.1f%%", v / 100.0);
        return String.format("%,d", v);
    }

    private void renderList()
    {
        _listPanel.removeAll();
        int rowH = 20;
        int y = 0;

        if (_fetchedKey < 0)
        {
            addRow("Loading...", "", y, new Color(140, 150, 160));
        }
        else if (_top.isEmpty())
        {
            addRow("No scores yet", "", y, new Color(140, 150, 160));
        }
        else
        {
            for (LeaderboardEntry e : _top)
            {
                addRow(e.rank + ". " + e.name, formatBoardScore(e.score), y, Colors.TEXT_COLOR);
                y += rowH;
            }

            boolean selfInTop = _self != null && _top.stream().anyMatch(e -> e.rank == _self.rank);
            if (_self != null && !selfInTop)
            {
                addRow("...", "", y, new Color(110, 118, 130));
                y += rowH;
                addRow(_self.rank + ". " + _self.name + " (you)", formatBoardScore(_self.score), y, new Color(0x66, 0xCC, 0xFF));
                y += rowH;
            }
        }

        _listPanel.revalidate();
        _listPanel.repaint();
    }

    private void addRow(String left, String right, int y, Color color)
    {
        Font font = new Font("SansSerif", Font.PLAIN, 12);

        JLabel lblLeft = new JLabel(left);
        lblLeft.setBounds(0, y, _width - 80, 18);
        lblLeft.setFont(font);
        lblLeft.setForeground(color);
        _listPanel.add(lblLeft);

        if (!right.isEmpty())
        {
            JLabel lblRight = new JLabel(right, SwingConstants.RIGHT);
            lblRight.setBounds(_width - 80, y, 80, 18);
            lblRight.setFont(font);
            lblRight.setForeground(color);
            _listPanel.add(lblRight);
        }
    }
}
