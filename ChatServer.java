import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class ChatClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JComboBox<String> usersList;
    private ChatClient client;

    public ChatClientGUI(String username, String host, int port) {
        setTitle("Czat - " + username);
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        inputField = new JTextField();
        usersList = new JComboBox<>();
        usersList.addItem("Wszyscy");
        JButton sendButton = new JButton("Wyślij");

        JPanel panelBottom = new JPanel(new BorderLayout());
        panelBottom.add(usersList, BorderLayout.WEST);
        panelBottom.add(inputField, BorderLayout.CENTER);
        panelBottom.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(panelBottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        try {
            client = new ChatClient(host, port, username,
                    msg -> SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n")),
                    users -> SwingUtilities.invokeLater(() -> updateUsers(users)));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nie można połączyć z serwerem.");
            System.exit(1);
        }
    }

    private void updateUsers(String[] users) {
        usersList.removeAllItems();
        usersList.addItem("Wszyscy");
        for (String u : users) { usersList.addItem(u); }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            String target = (String) usersList.getSelectedItem();
            if (target != null && !"Wszyscy".equals(target)) {
                client.sendMessage("@" + target + " " + text);
            } else {
                client.sendMessage(text);
            }
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        // Ustawiamy login testowy, żeby GUI zawsze się odpalało
        String username = "Marek"; // zmień na inny przy drugim kliencie
        SwingUtilities.invokeLater(() -> new ChatClientGUI(username, "localhost", 6000).setVisible(true));
    }
}

// =================== KLIENT =========================
class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> messageHandler;
    private Consumer<String[]> userListHandler;

    public ChatClient(String host, int port, String username,
                      Consumer<String> messageHandler,
                      Consumer<String[]> userListHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.userListHandler = userListHandler;
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(this::listen).start();

        if ("PODAJ_LOGIN".equals(in.readLine())) {
            out.println(username);
        }
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MSG:")) { messageHandler.accept(line.substring(4)); }
                else if (line.startsWith("USERS:")) {
                    String[] users = line.substring(6).split(",");
                    userListHandler.accept(users);
                }
            }
        } catch (IOException e) {
            messageHandler.accept("Połączenie z serwerem zakończone.");
        }
    }

    public void sendMessage(String message) { out.println(message); }
    public void close() { try { socket.close(); } catch (IOException e) {} }
}

