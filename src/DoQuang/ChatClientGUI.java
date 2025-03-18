package DoQuang;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatClientGUI extends JFrame implements ChatListener {
    private JTextField txtHost, txtPort, txtUsername, txtMessage;
    private JTextArea chatArea;
    private JButton btnConnect, btnSend;
    private ChatClient client;

    public ChatClientGUI() {
        setTitle("Chat Client");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel nhập host, port, username
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(3, 2));
        topPanel.add(new JLabel("Host:"));
        txtHost = new JTextField("localhost");
        topPanel.add(txtHost);

        topPanel.add(new JLabel("Port:"));
        txtPort = new JTextField("12345");
        topPanel.add(txtPort);

        topPanel.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        topPanel.add(txtUsername);
        add(topPanel, BorderLayout.NORTH);

        // Khu vực hiển thị tin nhắn
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Khu vực nhập tin nhắn và gửi
        JPanel bottomPanel = new JPanel(new BorderLayout());
        txtMessage = new JTextField();
        btnSend = new JButton("Send");
        btnSend.setEnabled(false); // Chỉ bật khi đã kết nối
        bottomPanel.add(txtMessage, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Nút kết nối
        btnConnect = new JButton("Connect");
        add(btnConnect, BorderLayout.WEST);

        // Xử lý sự kiện nút kết nối
        btnConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        // Xử lý sự kiện gửi tin nhắn
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        setVisible(true);
    }

    private void connectToServer() {
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        client = new ChatClient(host, port, this);
        client.connect();

        btnConnect.setEnabled(false);
        btnSend.setEnabled(true);
    }

    private void sendMessage() {
        String message = txtMessage.getText().trim();
        if (!message.isEmpty()) {
            client.sendMessage(txtUsername.getText(), message);
            txtMessage.setText("");
        }
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        chatArea.append(message.getSender() + ": " + message.getMessage() + "\n");
    }

    public static void main(String[] args) {
        new ChatClientGUI();
    }
}
