package com.voiceserver.server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ServerGUI extends JFrame {

    private DefaultTableModel tableModel;
    private JTable clientTable;
    private JTextArea logArea;
    private JButton banButton;

    public ServerGUI() {
        setTitle("SERVER CONTROL PANEL - VOICE CHAT");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Bảng danh sách Client kết nối
        String[] columns = {"Session ID", "Địa chỉ IP"};
        tableModel = new DefaultTableModel(columns, 0);
        clientTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(clientTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Danh sách Client online"));

        // 2. Nút Ban/Kick Client
        banButton = new JButton("CẤM / BAN CLIENT NÀY");
        banButton.setBackground(Color.RED);
        banButton.setForeground(Color.WHITE);
        banButton.setFocusable(false);
        banButton.addActionListener(e -> banSelectedClient());

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        leftPanel.add(banButton, BorderLayout.SOUTH);

        // 3. Khung Nhật ký (Log System)
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Nhật ký hệ thống"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, logScroll);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public void addClientToList(String sessionId, String ip) {
        SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{sessionId, ip}));
    }

    public void removeClientFromList(String sessionId) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(sessionId)) {
                    tableModel.removeRow(i);
                    break;
                }
            }
        });
    }

    private void banSelectedClient() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow != -1) {
            String sessionId = (String) tableModel.getValueAt(selectedRow, 0);
            VoiceServerHandler.banClient(sessionId);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 Client trong bảng để Ban!");
        }
    }
}