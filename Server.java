import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void privateMessage(String message, String targetUsername, ClientHandler sender) {
        boolean found = false;
        for (ClientHandler client : clientHandlers) {
            if (client.getUsername().equalsIgnoreCase(targetUsername)) {
                client.sendMessage("[Private] " + sender.getUsername() + ": " + message);
                found = true;
                break;
            }
        }
        if (!found) {
            sender.sendMessage("User '" + targetUsername + "' not found.");
        }
    }

    public static void addClient(ClientHandler client) {
        clientHandlers.add(client);
    }

    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your username:");
            username = in.readLine();

            Server.addClient(this);

            System.out.println(">> " + username + " joined the chat.");
            Server.broadcast(username + " has joined the chat!", this);

            String message;

            while ((message = in.readLine()) != null) {

                // 🟢 Print ALL messages in Server window
                System.out.println("Received from " + username + ": " + message);

                if (message.startsWith("@")) {

                    int spaceIndex = message.indexOf(' ');
                    if (spaceIndex != -1) {
                        String targetUser = message.substring(1, spaceIndex);
                        String privateMsg = message.substring(spaceIndex + 1);
                        Server.privateMessage(privateMsg, targetUser, this);
                    }

                } else {
                    Server.broadcast(username + ": " + message, this);
                }
            }

        } catch (IOException e) {
            System.out.println(">> " + username + " disconnected.");
        } finally {
            Server.removeClient(this);
            Server.broadcast(username + " has left the chat.", this);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
