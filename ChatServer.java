import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Serwer czatu uruchomiony na porcie " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, String fromUser) {
        synchronized (clients) {
            for (ClientHandler client : clients.values()) {
                client.sendMessage(fromUser + ": " + message);
            }
        }
    }

    static void privateMessage(String message, String fromUser, String toUser) {
        ClientHandler recipient = clients.get(toUser);
        if (recipient != null) {
            recipient.sendMessage("(Priv od " + fromUser + "): " + message);
        }
    }

    static void updateUserList() {
        String users = String.join(",", clients.keySet());
        synchronized (clients) {
            for (ClientHandler client : clients.values()) {
                client.sendUserList(users);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("PODAJ_LOGIN");
                userName = in.readLine();
                synchronized (clients) {
                    clients.put(userName, this);
                }
                System.out.println(userName + " dołączył do czatu.");
                broadcast("dołączył do czatu.", userName);
                updateUserList();

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("@")) {
                        // Wiadomość prywatna
                        int firstSpace = input.indexOf(' ');
                        if (firstSpace != -1) {
                            String targetUser = input.substring(1, firstSpace);
                            String msg = input.substring(firstSpace + 1);
                            privateMessage(msg, userName, targetUser);
                        }
                    } else {
                        broadcast(input, userName);
                    }
                }
            } catch (IOException e) {
                System.out.println(userName + " rozłączony.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) { }
                synchronized (clients) {
                    clients.remove(userName);
                }
                broadcast("opuścił czat.", userName);
                updateUserList();
            }
        }

        void sendMessage(String message) {
            out.println("MSG:" + message);
        }

        void sendUserList(String users) {
            out.println("USERS:" + users);
        }
    
