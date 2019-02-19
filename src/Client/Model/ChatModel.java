package Client.Model;

import Client.Controller.RoomController;

import java.io.*;
import java.net.*;

public class ChatModel extends Thread {
    private String ServerIP;
    private String username;
    private int port;
    PrintWriter sender;
    BufferedReader getter;
    Socket socket;
    boolean stopChat = false;
    RoomController parentThread;
    ListenerThread listener;

    public void stopChatThread() {
        stopChat = true;
    }

    public ChatModel(String ServerIP, int port, String username, RoomController parentThread) throws IOException {
        this.ServerIP = ServerIP;
        this.port = port;
        this.parentThread = parentThread;
        this.socket = new Socket(ServerIP, port);
        this.username = username;
        sender = new PrintWriter(socket.getOutputStream(), true);
        getter = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sender.println(username);
        listener = new ListenerThread();
        listener.start();
    }

    public void sendMessage(String message) {
        sender.println(message);
        sender.flush();
    }

    public void run() {
        try {
            while (!stopChat) {
                // ...
            }
            listener.shutdown();
            sender.println("[OFFLINE]");
            sender.close();
            getter.close();
            socket.close();
        } catch (Exception e) {
            return;
        }
    }

    private class ListenerThread extends Thread {
        private boolean stop = false;

        public void run() {
            while (!stop) {
                try {
                    String message = getter.readLine();
                    if (message == null) continue;
                    // System.out.println(message);
                    parentThread.receiveMessage(message);
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }
        }

        public void shutdown() {
            stop = true;
        }
    }
}