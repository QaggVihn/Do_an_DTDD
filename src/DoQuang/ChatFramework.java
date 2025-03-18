/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DoQuang;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Định nghĩa tin nhắn chat
class ChatMessage implements Serializable {
    private String sender;
    private String message;
    
    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }
    
    public String getSender() { return sender; }
    public String getMessage() { return message; }
}

// Server xử lý nhiều client
class ChatServer {
    private int port;
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    
    public ChatServer(int port) {
        this.port = port;
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcast(ChatMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    private class ClientHandler implements Runnable {
        private Socket socket;
        private ChatServer server;
        private ObjectOutputStream out;
        
        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }
        
        public void sendMessage(ChatMessage message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out = new ObjectOutputStream(socket.getOutputStream());
                while (true) {
                    ChatMessage message = (ChatMessage) in.readObject();
                    server.broadcast(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException e) { }
                server.clients.remove(this);
            }
        }
    }
}

// Interface để client nhận tin nhắn từ server
interface ChatListener {
    void onMessageReceived(ChatMessage message);
}

// Client kết nối đến server
class ChatClient {
    private String host;
    private int port;
    private ChatListener listener;
    private ObjectOutputStream out;
    
    public ChatClient(String host, int port, ChatListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }
    
    public void connect() {
        try {
            Socket socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            new Thread(() -> {
                try {
                    while (true) {
                        ChatMessage message = (ChatMessage) in.readObject();
                        listener.onMessageReceived(message);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendMessage(String sender, String message) {
        try {
            out.writeObject(new ChatMessage(sender, message));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

