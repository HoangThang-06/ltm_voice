package com.voiceserver.server;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceServerHandler extends AbstractWebSocketHandler {

    private static final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    // ➕ THÊM: Map lưu Username tương ứng với Session ID
    private static final Map<String, String> sessionUsernames = new ConcurrentHashMap<>();
    private static final Set<String> bannedIps = ConcurrentHashMap.newKeySet();
    private static ServerGUI gui;

    public static void setGUI(ServerGUI serverGUI) {
        gui = serverGUI;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientIp = session.getRemoteAddress().getAddress().getHostAddress();

        if (bannedIps.contains(clientIp)) {
            session.sendMessage(new TextMessage("BANNED"));
            session.close();
            if (gui != null) gui.log("Từ chối kết nối từ IP bị Cấm: " + clientIp);
            return;
        }

        // Tăng giới hạn kích thước gói tin Binary cho Session
        session.setBinaryMessageSizeLimit(64 * 1024);

        activeSessions.put(session.getId(), session);
        if (gui != null) {
            gui.addClientToList(session.getId(), clientIp);
            gui.log("Client mới kết nối [ID: " + session.getId() + " - IP: " + clientIp + "]");
        }
    }

    // ➕ THÊM: Phương thức xử lý tin nhắn Text từ Client (Đăng ký tên & trả về danh sách User)
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.startsWith("USER:")) {
            String username = payload.substring(5).trim();
            sessionUsernames.put(session.getId(), username);

            // 1. Gửi toàn bộ danh sách User đang online cho Client mới kết nối
            StringBuilder userListMsg = new StringBuilder("USER_LIST:");
            for (String u : sessionUsernames.values()) {
                userListMsg.append(u).append(",");
            }
            session.sendMessage(new TextMessage(userListMsg.toString()));

            // 2. Thông báo cho tất cả các Client khác biết có người dùng mới tham gia
            broadcastText("USER_JOIN:" + username, session.getId());

            if (gui != null) {
                gui.log("Client [ID: " + session.getId() + "] đã đăng ký tên: " + username);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // Trích xuất dữ liệu âm thanh ra mảng byte độc lập để tránh trôi con trỏ ByteBuffer
        ByteBuffer payload = message.getPayload();
        byte[] audioData = new byte[payload.remaining()];
        payload.get(audioData);

        if (audioData.length == 0) return;

        // Broadcast dữ liệu âm thanh tới các Client còn lại
        for (WebSocketSession s : activeSessions.values()) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                synchronized (s) {
                    try {
                        // Tạo đối tượng BinaryMessage mới từ mảng byte đã chép
                        s.sendMessage(new BinaryMessage(audioData));
                    } catch (Exception e) {
                        if (gui != null) {
                            gui.log("Lỗi chuyển tiếp voice tới [ID: " + s.getId() + "]: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = sessionUsernames.remove(session.getId());
        activeSessions.remove(session.getId());

        if (username != null) {
            // ➕ THÊM: Thông báo cho các Client còn lại biết người dùng này đã rời đi
            broadcastText("USER_LEAVE:" + username, session.getId());
        }

        if (gui != null) {
            gui.removeClientFromList(session.getId());
            gui.log("Client ngắt kết nối [ID: " + session.getId() + "]");
        }
    }

    // ➕ THÊM: Hàm phụ trợ phát tin nhắn dạng chữ tới các Client ngoại trừ 1 session
    private void broadcastText(String textMsg, String excludeSessionId) {
        TextMessage msg = new TextMessage(textMsg);
        for (WebSocketSession s : activeSessions.values()) {
            if (s.isOpen() && !s.getId().equals(excludeSessionId)) {
                try {
                    s.sendMessage(msg);
                } catch (IOException ignored) {}
            }
        }
    }

    public static void banClient(String sessionId) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null) {
            String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
            String username = sessionUsernames.remove(sessionId);
            bannedIps.add(clientIp);

            try {
                session.sendMessage(new TextMessage("KICKED"));
                session.close();
            } catch (IOException ignored) {}

            activeSessions.remove(sessionId);

            if (username != null) {
                // Thông báo tới các Client khác xóa ô đại diện của người bị Ban
                TextMessage leaveMsg = new TextMessage("USER_LEAVE:" + username);
                for (WebSocketSession s : activeSessions.values()) {
                    if (s.isOpen()) {
                        try { s.sendMessage(leaveMsg); } catch (IOException ignored) {}
                    }
                }
            }

            if (gui != null) {
                gui.removeClientFromList(sessionId);
                gui.log(">>> ĐÃ CẤM (BAN) Client ID: " + sessionId + " (IP: " + clientIp + ")");
            }
        }
    }
}
