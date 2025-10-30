package chatserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatClientGUI extends JFrame {
    private String login;
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JList<String> userList = new JList<>();
    private Map<String, PrivateChatWindow> privateChats = new HashMap<>();
    
    public ChatClientGUI() {
        setTitle("Chat Online");
        setSize(650, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); 
        setLayout(new BorderLayout());
        
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        JButton newClientBtn = new JButton("Nowy klient");
        newClientBtn.addActionListener(e -> new ChatClientGUI());
        buttonPanel.add(newClientBtn);
        
        JButton logoutBtn = new JButton("Wyloguj");
        logoutBtn.setBackground(new Color(220, 53, 69));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Czy na pewno chcesz się wylogować?",
                "Potwierdzenie wylogowania",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                dispose();
            }
        });
        buttonPanel.add(logoutBtn);
        
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Użytkownicy online:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        
        login = JOptionPane.showInputDialog(this, "Podaj login:");
        if (login == null || login.isEmpty()) {
            login = "Anonim" + (int)(Math.random() * 1000);
        }
        
        setTitle("Chat Online - " + login);
        
        ChatServerMemory.registerClient(this);
        
        inputField.addActionListener(e -> {
            String msg = inputField.getText().trim();
            inputField.setText("");
            if (!msg.isEmpty()) {
                ChatServerMemory.broadcast(login + ": " + msg);
            }
        });
        
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(login)) {
                        openPrivateChat(selectedUser);
                    }
                }
            }
        });
        
        setVisible(true);
    }
    
    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    public void updateUserList(String[] users) {
        userList.setListData(users);
    }
    
    public String getLogin() {
        return login;
    }
    
    public PrivateChatWindow openPrivateChat(String user) {
        if (!privateChats.containsKey(user)) {
            PrivateChatWindow window = new PrivateChatWindow(this, user);
            privateChats.put(user, window);
        } else {
            PrivateChatWindow window = privateChats.get(user);
            if (!window.isVisible()) {
                window.setVisible(true);
            }
            window.toFront();
            window.requestFocus();
        }
        return privateChats.get(user);
    }
    
    public void receivePrivateMessage(String from, String message) {
        PrivateChatWindow window = openPrivateChat(from);
        window.appendMessage(from + ": " + message);
        
        if (!window.isActive()) {
            window.flashTitle();
        }
    }
    
    public void sendPrivate(String to, String message) {
        ChatServerMemory.sendPrivate(login, to, message);
    }
    
    public void removePrivateChat(String user) {
    }
    
    @Override
    public void dispose() {
        for (PrivateChatWindow window : privateChats.values()) {
            window.dispose();
        }
        ChatServerMemory.unregisterClient(this);
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClientGUI();
        });
    }
}