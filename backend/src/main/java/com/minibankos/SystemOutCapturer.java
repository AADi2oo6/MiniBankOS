package com.minibankos;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.OutputStream;
import java.io.PrintStream;

@Component
public class SystemOutCapturer {

    private final SimpMessagingTemplate messagingTemplate;

    public SystemOutCapturer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                originalOut.write(b); // Keep printing to standard console
                if (b == '\n') {
                    String message = buffer.toString();
                    buffer.setLength(0); // Reset buffer
                    // Broadcast to websocket
                    messagingTemplate.convertAndSend("/topic/os-events", message);
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }
        }, true)); // Auto-flush true
    }
}
