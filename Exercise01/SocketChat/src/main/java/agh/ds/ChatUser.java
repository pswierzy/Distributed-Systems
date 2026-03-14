package agh.ds;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Objects;
import java.util.Scanner;

public class ChatUser {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 9090;

    private static final String MULTICAST_ADDRESS = "230.1.1.1";
    private static final int MULTICAST_PORT = 9091;

    private static String AsciiArt =
                    """
                      /|、
                     (°､ 。7
                      |、~ヽノシ
                      じじと)\
                    """;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write your name: ");
        String nick = scanner.nextLine();
        boolean inMulticastGroup = true;

        try {
            // TCP
            Socket tcpSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            PrintWriter outTCP = new PrintWriter(tcpSocket.getOutputStream(), true);
            BufferedReader inTCP = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

            // UDP
            DatagramSocket udpSocket = new DatagramSocket();

            // serwer
            outTCP.println(nick);
            outTCP.println(udpSocket.getLocalPort());

            // Multicast
            MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
            //multicastSocket.setLoopbackMode(true);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);

            System.out.println("--- Connected to chat ---");
            System.out.println("Normal text -> sends via TCP");
            System.out.println("Command 'U' -> sends ASCII Art via UDP");
            System.out.println("Command 'M' -> sends ASCII Art via Multicast");
            System.out.println("Command '+M' -> joins Multicast group");
            System.out.println("Command '-M' -> leaves Multicast group");
            System.out.println("Command 'A' -> enter a new Ascii art or message for 'U' and 'M' commands");
            System.out.println("--------------------------");

            // Wątek TCP
            new Thread(() -> {
                try {
                    String mess;
                    while((mess = inTCP.readLine()) != null) {
                        System.out.println(mess);
                    }
                } catch(IOException e) {
                    System.out.println("Disconnected TCP.");
                }
            }).start();

            // Wątek UDP
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while(true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        String mess = new String(packet.getData(), 0, packet.getLength());
                        System.out.println(mess);
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Wątek Multicast
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while(true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSocket.receive(packet);
                        String mess = new String(packet.getData(), 0, packet.getLength());

                        if(mess.startsWith("[MULTICAST] " + nick)) {
                            continue;
                        }

                        System.out.println(mess);
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Wątek wysyłania
            while (true) {
                String input = scanner.nextLine();

                if(input.equalsIgnoreCase("U")) {
                    byte[] data = AsciiArt.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                    udpSocket.send(packet);
                    System.out.println("[Sent UDP]");

                } else if(input.equalsIgnoreCase("M")) {
                    if(!inMulticastGroup) {
                        System.out.println("Not connected to Multicast group");
                        continue;
                    }
                    String multiMess = "[MULTICAST] " + nick + ":\n" + AsciiArt;
                    byte[] data = multiMess.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
                    multicastSocket.send(packet);
                    System.out.println("[Sent Multicast]");

                } else if(input.equalsIgnoreCase("+M")) {
                    // Dołączanie do Multicastu
                    if (!inMulticastGroup) {
                        multicastSocket.joinGroup(group);
                        inMulticastGroup = true;
                        System.out.println("[Joined Multicast]");
                    } else {
                        System.out.println("[You are already in Multicast]");
                    }

                } else if(input.equalsIgnoreCase("-M")) {
                    // Opuszczanie Multicastu
                    if (inMulticastGroup) {
                        multicastSocket.leaveGroup(group);
                        inMulticastGroup = false;
                        System.out.println("[Left multicast]");
                    } else {
                        System.out.println("[You are already not in Multicast]");
                    }

                }else if(input.equalsIgnoreCase("A")) {
                    System.out.println("Enter new broadcast and multicast message \n(finish with a single A in a new line):");
                    StringBuilder newASCII = new StringBuilder();

                    while (true) {
                        String line = scanner.nextLine();
                        if (line.equalsIgnoreCase("A")) {
                            break;
                        }
                        newASCII.append(line).append("\n");
                    }

                    // Zapisujemy nowe ASCII
                    AsciiArt = newASCII.toString();
                    System.out.println("[Updated ASCII Art]");

                } else {
                    outTCP.println(input);
                }
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
