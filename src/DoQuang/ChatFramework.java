package DoQuang;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


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

class ChatServer {
    private int port;
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private int connectedClients = 0; 
    private List<ServerChatListener> listeners = new ArrayList<>();

    public ChatServer(int port) {
        this.port = port;
    }
    
    public void addServerChatListener(ServerChatListener listener) {
        listeners.add(listener);
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
                    connectedClients++; 
                    System.out.println("New client connected. Total clients: " + connectedClients);                 
                    new Thread(clientHandler).start();                   
                } catch (IOException e) {
                    if (!running) break; 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
    public void stop() {
        broadcast(new ChatMessage("Server", "Closing Server..."));
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            connectedClients = 0; 
            System.out.println("Server stopped. Total clients: " + connectedClients);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcast(ChatMessage message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        for (ServerChatListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }
    
   
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
                server.connectedClients--; 
                System.out.println("Client disconnected. Total clients: " + server.connectedClients);
                close();
            }
        }
    }
}


interface ChatListener {
    void onMessageReceived(ChatMessage message);
}
interface ServerChatListener {
    void onMessageReceived(ChatMessage message);
}

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
                    
                } catch (IOException | ClassNotFoundException e) {
                    if (running) e.printStackTrace();
                } finally {
                    if (running) disconnect(); 
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();           
        }
    }
    //ham ngat ket noi
    public void disconnect() {        
        running = false; 
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
