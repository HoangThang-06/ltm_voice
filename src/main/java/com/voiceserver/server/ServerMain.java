package com.voiceserver.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.swing.*;

@SpringBootApplication
@EnableWebSocket
public class ServerMain implements WebSocketConfigurer {

    public static void main(String[] args) {
        // Cho phép bật GUI Swing cùng với Spring Boot
        SpringApplicationBuilder builder = new SpringApplicationBuilder(ServerMain.class);
        builder.headless(false);
        builder.run(args);

        // Khởi chạy Giao diện Control Panel Server
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            VoiceServerHandler.setGUI(gui);
            gui.setVisible(true);
            gui.log("Server đã khởi tạo thành công tại cổng 8080!");
        });
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new VoiceServerHandler(), "/voice").setAllowedOrigins("*");
    }
}