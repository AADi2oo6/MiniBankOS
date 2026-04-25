import Auth.AuthManager;
import kernel.ModeBit;
import kernel.RecoveryManager;
import kernel.scheduler.Scheduler;
import shell.*;
import system.BankDatabase;
import kernel.TransactionManager;

import java.io.*;
import java.net.*;

/**
 * MiniBankOS Server -- Multi-client TCP server on port 9999
 *
 * Each client connects and gets their own session (ThreadLocal).
 * All clients share ONE TransactionManager → ONE BankersAlgorithm instance.
 * Output routes to both server console AND the originating client.
 *
 * Start: java -cp out Server
 * Then open clients: java -cp out Client
 */
public class Server {
    public static final int PORT = 9999;

    public static void main(String[] args) throws Exception {
        // Install ThreadLocalPrintStream so each client's output goes back to them
        ThreadLocalPrintStream tlps = new ThreadLocalPrintStream(System.out);
        System.setOut(tlps);

        // One shared TransactionManager (one shared BankersAlgorithm)
        BankDatabase bank = new BankDatabase();
        Scheduler scheduler = new Scheduler();
        ModeBit modeBit = new ModeBit();
        AuthManager authManager = new AuthManager();
        TransactionManager tm = new TransactionManager(bank, scheduler, authManager, modeBit);

        RecoveryManager recovery = new RecoveryManager(tm);
        recovery.recover();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("  ====================================================");
        System.out.println("  MiniBankOS Server started on port " + PORT);
        System.out.println("  Waiting for clients... (run: java -cp out Client)");
        System.out.println("  ====================================================");

        int clientCount = 0;
        while (true) {
            Socket socket = serverSocket.accept();
            int id = ++clientCount;
            System.out.println("  [SERVER] Client #" + id + " connected from " + socket.getInetAddress());

            Thread clientThread = new Thread(() -> handleClient(socket, id, tm, authManager));
            clientThread.setName("Client-" + id);
            clientThread.start();
        }
    }

    private static void handleClient(Socket socket, int id, TransactionManager tm, AuthManager authManager) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintStream    out = new PrintStream(socket.getOutputStream(), true)
        ) {
            // Route this thread's System.out calls to the client socket too
            ThreadLocalPrintStream.setClientStream(out);

            out.println("  ====================================================");
            out.println("  MiniBankOS Server -- Client #" + id);
            out.println("  Type commands. Type 'exit' to disconnect.");
            out.println("  ====================================================");

            CommandParser parser = new CommandParser(tm);
            String line;

            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("exit")) {
                    out.println("Disconnected from server.");
                    System.out.println("  [SERVER] Client #" + id + " disconnected.");
                    break;
                }

                // Handle login/logout inline (needs auth + session)
                String[] tokens = line.split("\\s+");

                if (tokens[0].equalsIgnoreCase("login") && tokens.length == 3) {
                    Auth.user u = authManager.login(tokens[1], tokens[2]);
                    if (u != null) {
                        Auth.Session.login(u);
                        out.println("Login successful. Welcome, " + u.getUsername());
                    } else {
                        out.println("Invalid credentials.");
                    }
                } else if (tokens[0].equalsIgnoreCase("logout")) {
                    Auth.Session.logout();
                    out.println("Logged out.");
                } else {
                    // All other commands go through CommandParser
                    out.print("BankingOS@" + (Auth.Session.isLoggedIn() ? Auth.Session.getCurrentUser().getUsername() : "guest") + "> ");
                    out.flush();
                    parser.parse(line);
                }
                out.println();
            }
        } catch (IOException e) {
            System.out.println("  [SERVER] Client #" + id + " disconnected (connection reset).");
        } finally {
            ThreadLocalPrintStream.clearClientStream();
        }
    }
}
