package org.example.ex1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Client {
    public static void main(String args[]) throws Exception
    {
        System.out.println("CLIENT STARTED");
        DatagramSocket socket = null;
        int portNumber = 9008;
        int pongNumber = 9009;
        byte[] receiveBuffer = new byte[1024];

        try {
            socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName("localhost");
            byte[] sendBuffer = "Ping".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
            socket.send(sendPacket);
            System.out.println("SENT PING");

            DatagramSocket receiveSocket = new DatagramSocket(pongNumber);
            Arrays.fill(receiveBuffer, (byte)0);
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            receiveSocket.receive(receivePacket);
            String msg = new String(receivePacket.getData());
            System.out.println("received msg: " + msg.trim());
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
