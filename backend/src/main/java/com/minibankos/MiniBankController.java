package com.minibankos;

import Auth.AuthManager;
import Auth.Session;
import kernel.TransactionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shell.CommandParser;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For local React dev
public class MiniBankController {

    private final CommandParser parser;
    private final AuthManager authManager;

    public MiniBankController(TransactionManager transactionManager, AuthManager authManager) {
        this.parser = new CommandParser(transactionManager);
        this.authManager = authManager;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeCommand(@RequestBody Map<String, String> payload) {
        String command = payload.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty command"));
        }

        command = command.trim();
        String[] tokens = command.split("\\s+");
        String cmd = tokens[0].toLowerCase();

        // Handle login manually so we can manage Session if needed, though CommandParser handles it too.
        // We'll just let CommandParser handle most, but login/logout specifically modifies global state.
        if (cmd.equals("login")) {
            parser.parse(command);
            if (Session.isLoggedIn()) {
                return ResponseEntity.ok(Map.of("status", "success", "user", Session.getCurrentUser().getUsername()));
            } else {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Invalid credentials"));
            }
        } 
        else if (cmd.equals("logout")) {
            parser.parse(command);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Logged out"));
        }
        else if (cmd.equals("whoami")) {
             if (Session.isLoggedIn()) {
                return ResponseEntity.ok(Map.of("status", "success", "user", Session.getCurrentUser().getUsername()));
             } else {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Not logged in"));
             }
        }

        // For all other commands
        parser.parse(command);
        return ResponseEntity.ok(Map.of("status", "success", "command", command));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(authManager.getAllUsers());
    }
}
