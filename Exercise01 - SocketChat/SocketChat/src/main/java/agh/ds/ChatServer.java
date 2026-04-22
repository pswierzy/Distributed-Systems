package agh.ds;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 9090;
    private static final List<UserHandler> users = new CopyOnWriteArrayList<>();
    private static DatagramSocket udpSocket;

    public static void main(String[] args) {
        System.out.println("Server starting...");

        ExecutorService threadPool = Executors.newCachedThreadPool();

        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            udpSocket = new DatagramSocket(PORT);
            System.out.println("Server listening on port " + PORT);

            // UDP
            threadPool.execute(ChatServer::handleUDP);

            // TCP
            while(true) {
                Socket userSocket = serverSocket.accept();
                UserHandler userHandler = new UserHandler(userSocket, users);
                users.add(userHandler);

                threadPool.execute(userHandler);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    // Obsługa UDP
    private static void handleUDP() {
        // odbiór wiadomości -> analiza kto wysłał -> wysłanie wszystkim
        byte[] buffer = new byte[1024];
        while(true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String mess = new String(packet.getData(), 0, packet.getLength());

                String sender = "Unknown";
                for(UserHandler user : users) {
                    if(user.getAddress().equals(packet.getAddress()) && user.getUdpPort() == packet.getPort()) {
                        sender = user.getNick();
                        break;
                    }
                }

                String broadcastMess = "[UDP] " + sender + ":\n" + mess;
                System.out.println(broadcastMess);
                byte[] broadcastBytes = broadcastMess.getBytes();


                for(UserHandler user : users) {
                    if(user.getAddress().equals(packet.getAddress()) && user.getUdpPort() == packet.getPort()) {
                        continue;
                    }
                    DatagramPacket newPacket = new DatagramPacket(broadcastBytes, broadcastBytes.length, user.getAddress(), user.getUdpPort());
                    udpSocket.send(newPacket);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
