package com.rabbitmq;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Admin {
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare("exchange_services", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_confirmations", BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare("exchange_admin", BuiltinExchangeType.TOPIC);

        // nasłuch na wszystkie wiadomości
        String adminQueue = channel.queueDeclare().getQueue();
        channel.queueBind(adminQueue, "exchange_services", "service.#");
        channel.queueBind(adminQueue, "exchange_confirmations", "agency.#");

        DeliverCallback deliverCallback = (_, delivery) -> {
            String exchange = delivery.getEnvelope().getExchange();
            String routingKey = delivery.getEnvelope().getRoutingKey();
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[LOG] Przechwycono z " + exchange + " (klucz: " + routingKey + ") -> " + message);
        };
        channel.basicConsume(adminQueue, true, deliverCallback, _ -> {});

        // wysyłanie wiadomości
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Wyślij wiadomość jako admin:");
        System.out.println("<cel> <wiadomość>");
        System.out.println("Cele: 'agency', 'carrier', 'all'");
        System.out.println("Wpisz 'exit', aby zakończyć");

        while (true) {
            String input = reader.readLine();
            if ("exit".equalsIgnoreCase(input)) break;

            String[] parts = input.split(" ");
            String target = parts[0];
            String msg = parts[1];
            String routingKey;

            switch (target) {
                case "agency":
                    routingKey = "admin.agency";
                    break;
                case "carrier":
                    routingKey = "admin.carrier";
                    break;
                case "all":
                    routingKey = "admin.all";
                    break;
                default:
                    System.out.println("Błąd: Nieznany cel");
                    continue;
            }

            channel.basicPublish("exchange_admin", routingKey, null, msg.getBytes(StandardCharsets.UTF_8));
            System.out.println("[+] Wiadomość wysłana do " + target + " -> " + msg);
        }

        connection.close();
        System.exit(0);
    }
}
