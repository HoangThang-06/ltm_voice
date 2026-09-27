package com.voiceserver.server;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceServerHandler extends AbstractWebSocketHandler {

    private static final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private static final Set<String> bannedIps = ConcurrentHashMap.newKeySet();
    private static ServerGUI gui;

    public static void setGUI(ServerGUI serverGUI) {
        gui = serverGUI;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientIp = session.getRemoteAddress().getAddress().getHostAddress();

        // Kiểm tra xem IP có nằm trong danh sách cấm hay không
        if (bannedIps.contains(clientIp)) {
            session.sendMessage(new TextMessage("BANNED"));
            session.close();
            if (gui != null) gui.log("Từ chối kết nối từ IP bị Cấm: " + clientIp);
            return;
        }

        activeSessions.put(session.getId(), session);
        if (gui != null) {
            gui.addClientToList(session.getId(), clientIp);
            gui.log("Client mới kết nối [ID: " + session.getId() + " - IP: " + clientIp + "]");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        for (WebSocketSession s : activeSessions.values()) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                synchronized (s) { // Khóa session để tránh xung đột ghi dữ liệu đa luồng
                    try {
                        s.sendMessage(message);
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session.getId());
        if (gui != null) {
            gui.removeClientFromList(session.getId());
            gui.log("Client ngắt kết nối [ID: " + session.getId() + "]");
        }
    }

    // Hàm gọi khi Admin bấm nút BAN / KICK trên Server GUI
    public static void banClient(String sessionId) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null) {
            String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
            bannedIps.add(clientIp); // Thêm IP vào danh sách đen

            try {
                session.sendMessage(new TextMessage("KICKED"));
                session.close();
            } catch (IOException ignored) {}

            activeSessions.remove(sessionId);
            if (gui != null) {
                gui.removeClientFromList(sessionId);
                gui.log(">>> ĐÃ CẤM (BAN) Client ID: " + sessionId + " (IP: " + clientIp + ")");
            }
        }
    }
}