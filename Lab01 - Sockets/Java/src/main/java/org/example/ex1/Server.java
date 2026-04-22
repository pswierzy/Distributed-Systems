package org.example.ex1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;

public class Server {
    public static void main(String args[])
    {
        System.out.println("SERVER STARTED");
        DatagramSocket socket = null;
        int portNumber = 9008;
        int pongNumber = 9009;

        try{
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData());
                System.out.println("received msg: " + msg.trim());

                System.out.println("SENDING PONG BACK");

                byte[] sendBuffer = "Pong".getBytes();
                InetAddress address = receivePacket.getAddress();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, pongNumber);
                socket.send(sendPacket);
            }
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
