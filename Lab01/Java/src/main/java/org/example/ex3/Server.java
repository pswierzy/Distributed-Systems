package org.example.ex3;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Server {
    public static void main(String args[])
    {
        System.out.println("SERVER STARTED");
        DatagramSocket socket = null;
        int portNumber = 9011;
        int backPortNumber = 9012;

        try{
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);

                int nb = ByteBuffer.wrap(receivePacket.getData()).getInt();
                System.out.println("nb: " + nb);
                nb += 1;

                byte[] buff = ByteBuffer.allocate(4).putInt(nb).array();
                InetAddress address = receivePacket.getAddress();
                DatagramPacket sendPacket = new DatagramPacket(buff, buff.length, address, backPortNumber);
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