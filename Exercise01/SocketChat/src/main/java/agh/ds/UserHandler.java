package agh.ds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class UserHandler implements Runnable{
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nick;
    private final InetAddress address;
    private int udpPort;
    private final List<UserHandler> users;

    public UserHandler(Socket socket, List<UserHandler> users) {
        this.socket = socket;
        this.address = socket.getInetAddress();
        this.users = users;
    }

    public String getNick() {
        return nick;
    }
    public InetAddress getAddress() {
        return address;
    }
    public int getUdpPort() {
        return udpPort;
    }

    // Obsługa TCP
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.nick = in.readLine();
            this.udpPort = Integer.parseInt(in.readLine());

            System.out.println("Joined: " + nick + " [TCP: " + socket.getPort() + ", UDP: " + udpPort + "]");
            broadcastTCP("[Server]: " + nick + " joined chat.", this);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.print("[TCP] " + nick + ": " + message + "\n");
                broadcastTCP("[" + nick + "]: " + message, this);
            }
        } catch(IOException e) {

        } finally {
            disconnect();
        }
    }

    private void broadcastTCP(String message, UserHandler sender) {
        for(UserHandler user: users) {
            if(user != sender) {
                user.out.println(message);
            }
        }
    }

    private void disconnect() {
        users.remove(this);
        System.out.println(nick + " left the chat.");
        broadcastTCP("[Server]: " + nick + " left the chat.", this);
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
