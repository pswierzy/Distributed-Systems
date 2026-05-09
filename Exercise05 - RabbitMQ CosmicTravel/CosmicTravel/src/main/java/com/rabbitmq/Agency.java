package com.rabbitmq;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Agency {
    public static void main(String[] argv) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Podaj nazwę agencji: ");
        String agencyName = reader.readLine();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("exchange_services", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_confirmations", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_admin", BuiltinExchangeType.TOPIC);

        // odbiór potwierdzeń od przewoźników
        String confirmationsQueue = channel.queueDeclare().getQueue();
        channel.queueBind(confirmationsQueue, "exchange_confirmations", "agency." + agencyName);

        DeliverCallback confirmCallback = (_, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[-] Potwierdzenie: " + message);
        };
        channel.basicConsume(confirmationsQueue, true, confirmCallback, _ -> {});

        // odbiór wiadomości od admina
        String adminQueue = channel.queueDeclare().getQueue();
        channel.queueBind(adminQueue, "exchange_admin", "admin.agency");
        channel.queueBind(adminQueue, "exchange_admin", "admin.all");

        DeliverCallback adminCallback = (_, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[!] Admin: " + message);
        };
        channel.basicConsume(adminQueue, true, adminCallback, _ -> {});

        // wysyłanie do carriera
        System.out.println("Gotowy. Wpisz typ zlecenia (passenger, cargo, satellite) lub 'exit':");
        int taskCounter = 1;
        while (true) {
            String serviceType = reader.readLine();
            if ("exit".equalsIgnoreCase(serviceType)) break;

            if (!serviceType.matches("passenger|cargo|satellite")) {
                System.out.println("Błąd: Nieznany typ usługi.");
                continue;
            }

            String taskId = agencyName + "-" + taskCounter++;
            String routingKey = "service." + serviceType;
            String message = taskId + ":" + agencyName;

            channel.basicPublish("exchange_services", routingKey, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("[+] Wysłano: " + taskId + " (typ: " + serviceType + ")");
        }

        connection.close();
        System.exit(0);
    }
}
