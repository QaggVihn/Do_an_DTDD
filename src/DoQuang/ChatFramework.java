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
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private int connectedClients = 0; // Biến đếm số client đang kết nối

    public ChatServer(int port) {
        this.port = port;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port);
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket, this);
                    clients.add(clientHandler);
                    connectedClients++; // Tăng số đếm khi client kết nối
                    System.out.println("New client connected. Total clients: " + connectedClients);                 
                    new Thread(clientHandler).start();                   
                } catch (IOException e) {
                    if (!running) break; // Dừng nếu server đã bị tắt
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            connectedClients = 0; // Đặt lại số đếm khi server dừng
            System.out.println("Server stopped. Total clients: " + connectedClients);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcast(ChatMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    // Thêm phương thức để lấy số client đang kết nối
    public int getConnectedClients() {
        return connectedClients;
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
        
        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out = new ObjectOutputStream(socket.getOutputStream());
                
                
                broadcast(new ChatMessage("Server", "Someone has connected."));
                while (server.running) {
                    ChatMessage message = (ChatMessage) in.readObject();
                    server.broadcast(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                //e.printStackTrace();
            } finally {
                server.clients.remove(this);
                server.connectedClients--; // Giảm số đếm khi client ngắt kết nối
                System.out.println("Client disconnected. Total clients: " + server.connectedClients);
                close();
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
    Socket socket;
    private volatile boolean running = true;
    
    public ChatClient(String host, int port, ChatListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }
    
    public void connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            new Thread(() -> {
                try {
                    while (running) {
                        ChatMessage message = (ChatMessage) in.readObject();
                        listener.onMessageReceived(message);
                    }
                } catch (EOFException | SocketException e) {
                    // Bỏ qua lỗi khi socket đã đóng
                } catch (IOException | ClassNotFoundException e) {
                    if (running) e.printStackTrace();
                } finally {
                    if (running) disconnect(); // Chỉ gọi nếu chưa chủ động ngắt
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();           
        }
    }
    
    public void disconnect() {
        running = false; // Dừng vòng lặp nhận tin nhắn
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Client disconnected from server.");
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
