package com.voiceserver.server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerGUI extends JFrame {

    private DefaultTableModel tableModel;
    private JTable clientTable;
    private JTextArea logArea;
    private JButton banButton;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerGUI() {
        setTitle("SERVER CONTROL PANEL - VOICE CHAT");
        setSize(780, 450); // Mở rộng chiều rộng cửa sổ để hiển thị đủ 4 cột
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Bảng danh sách Client kết nối (Thêm cột Giờ vào & Giờ ra)
        String[] columns = {"Session ID", "Địa chỉ IP", "Giờ vào", "Giờ ra"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Vô hiệu hóa tính năng sửa trực tiếp trên bảng
            }
        };
        clientTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(clientTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Danh sách & Lịch sử Client"));

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
        splitPane.setDividerLocation(440); // Điều chỉnh vị trí vạch phân chia giữa Bảng và Log
        add(splitPane, BorderLayout.CENTER);
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    // Khi Client kết nối: Tự động ghi thời gian hiện tại vào cột "Giờ vào"
    public void addClientToList(String sessionId, String ip) {
        String joinTime = LocalTime.now().format(TIME_FORMATTER);
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{sessionId, ip, joinTime, "Đang kết nối"})
        );
    }

    // Khi Client ngắt kết nối / bị Ban: Cập nhật thời gian vào cột "Giờ ra"
    public void removeClientFromList(String sessionId) {
        String leaveTime = LocalTime.now().format(TIME_FORMATTER);
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(sessionId)) {
                    tableModel.setValueAt(leaveTime, i, 3); // Cập nhật cột chỉ số 3 ("Giờ ra")
                    break;
                }
            }
        });
    }

    private void banSelectedClient() {
        int selectedRow = clientTable.getSelectedRow();
        if (selectedRow != -1) {
            String sessionId = (String) tableModel.getValueAt(selectedRow, 0);
            String leaveStatus = (String) tableModel.getValueAt(selectedRow, 3);

            // Kiểm tra nếu Client đã thoát từ trước thì không cho Ban
            if (!"Đang kết nối".equals(leaveStatus)) {
                JOptionPane.showMessageDialog(this, "Client này đã ngắt kết nối từ trước!");
                return;
            }

            VoiceServerHandler.banClient(sessionId);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 Client trong bảng để Ban!");
        }
    }
}