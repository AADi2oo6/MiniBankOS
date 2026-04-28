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
    private final TransactionManager transactionManager;

    public MiniBankController(TransactionManager transactionManager, AuthManager authManager) {
        this.parser = new CommandParser(transactionManager);
        this.authManager = authManager;
        this.transactionManager = transactionManager;
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

    @GetMapping("/accounts/{userId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String userId) {
        Auth.user current = Session.getCurrentUser();
        if(current == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        Double bal = transactionManager.getBalanceValue(userId);
        if(bal == null) return ResponseEntity.status(403).body(Map.of("error", "Access denied or not found"));
        return ResponseEntity.ok(Map.of("balance", bal));
    }

    @GetMapping("/accounts/{userId}/history")
    public ResponseEntity<?> getHistory(@PathVariable String userId) {
        Auth.user current = Session.getCurrentUser();
        if(current == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        java.util.List<Map<String, String>> txns = transactionManager.getTransactionsList(userId);
        return ResponseEntity.ok(txns);
    }

    @GetMapping("/loans/rates")
    public ResponseEntity<?> getRates() {
        return ResponseEntity.ok(transactionManager.getRatesData());
    }

    @GetMapping("/loans/{userId}")
    public ResponseEntity<?> getLoans(@PathVariable String userId) {
        Auth.user current = Session.getCurrentUser();
        if(current == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        java.util.List<Map<String, Object>> loans = transactionManager.getLoansList(userId);
        return ResponseEntity.ok(loans);
    }
    
    @PostMapping("/os/simulate")
    public ResponseEntity<?> simulateScheduler(@RequestBody Map<String, Object> payload) {
        kernel.scheduler.Scheduler simulator = new kernel.scheduler.Scheduler();
        
        long quantum = Long.parseLong(payload.getOrDefault("quantum", "3").toString());
        java.util.List<Map<String, Object>> processesPayload = (java.util.List<Map<String, Object>>) payload.get("processes");
        
        int pidCounter = 1;
        if(processesPayload != null) {
            for (Map<String, Object> p : processesPayload) {
                String name = (String) p.getOrDefault("type", "Unknown");
                int priority = Integer.parseInt(p.getOrDefault("priority", "1").toString());
                long burst = Long.parseLong(p.getOrDefault("burst", "1").toString());
                long arrival = Long.parseLong(p.getOrDefault("arrival", "0").toString());
                
                kernel.scheduler.TransactionProcess tp = new kernel.scheduler.TransactionProcess(
                    "P" + (pidCounter++), 
                    () -> {}, // Simulated blank runtime execution 
                    priority, 
                    name, 
                    burst
                );
                tp.setArrivalTime(arrival);
                simulator.submitProcess(tp);
            }
        }
        
        simulator.runPendingProcesses(quantum);
        
        return ResponseEntity.ok(Map.of(
            "metrics", simulator.getHistory(),
            "gantt", simulator.getGanttSlices()
        ));
    }
}
