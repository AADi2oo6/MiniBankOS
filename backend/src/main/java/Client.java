import java.io.*;
import java.net.*;

/**
 * MiniBankOS Client -- connects to Server on localhost:9999
 *
 * Start server first: java -cp out Server
 * Then run this:      java -cp out Client
 *
 * Open multiple Client terminals to simulate concurrent users.
 */
public class Client {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = Server.PORT;

        Socket socket = new Socket(host, port);
        System.out.println("Connected to MiniBankOS Server at " + host + ":" + port);

        // Thread to read server responses and print them
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("[Disconnected from server]");
            }
        });
        reader.setDaemon(true);
        reader.start();

        // Read from user's keyboard and send to server
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String line;
        while ((line = keyboard.readLine()) != null) {
            out.println(line);
            if (line.trim().equalsIgnoreCase("exit")) break;
        }
        socket.close();
    }
}
