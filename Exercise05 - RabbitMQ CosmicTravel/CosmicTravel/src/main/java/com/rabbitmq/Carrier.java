package com.rabbitmq;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Carrier {
    public static void main(String[] argv) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Podaj nazwę przewoźnika: ");
        String carrierName = reader.readLine();

        System.out.print("Podaj usługę 1 (passenger, cargo, satellite): ");
        String service1 = reader.readLine();
        System.out.print("Podaj usługę 2 (passenger, cargo, satellite): ");
        String service2 = reader.readLine();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("exchange_services", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_confirmations", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_admin", BuiltinExchangeType.TOPIC);

        // odbiór serwisów od agencji
        String qService1 = "q_" + service1;
        String qService2 = "q_" + service2;

        channel.queueDeclare(qService1, true, false, false, null);
        channel.queueDeclare(qService2, true, false, false, null);

        channel.queueBind(qService1, "exchange_services", "service." + service1);
        channel.queueBind(qService2, "exchange_services", "service." + service2);

        DeliverCallback serviceCallback = (_, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] parts = message.split(":");
            String taskId = parts[0];
            String agencyName = parts[1];

            System.out.println("[-] Otrzymano: " + taskId + " od " + agencyName);

            System.out.println("[+] Wykonano: " + taskId);

            String confirmMessage = "Zlecenie " + taskId + " wykonane przez " + carrierName;
            String confirmRoutingKey = "agency." + agencyName;
            channel.basicPublish("exchange_confirmations", confirmRoutingKey, null, confirmMessage.getBytes(StandardCharsets.UTF_8));

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        channel.basicConsume(qService1, false, serviceCallback, _ -> {});
        channel.basicConsume(qService2, false, serviceCallback, _ -> {});

        // odbiór wiadomości od admina
        String adminQueue = channel.queueDeclare().getQueue();
        channel.queueBind(adminQueue, "exchange_admin", "admin.carrier");
        channel.queueBind(adminQueue, "exchange_admin", "admin.all");

        DeliverCallback adminCallback = (_, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[!] Admin: " + message);
        };
        channel.basicConsume(adminQueue, true, adminCallback, _ -> {});

        System.out.println("Gotowy. Nasłuchuje na " + service1 + " oraz " + service2 + ".");
    }
}
