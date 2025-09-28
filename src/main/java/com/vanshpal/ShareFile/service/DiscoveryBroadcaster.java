package com.vanshpal.ShareFile.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DiscoveryBroadcaster {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void start() {
        executor.scheduleAtFixedRate(this::broadcast, 0, 5, TimeUnit.SECONDS);
    }

    private void broadcast() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = "MYAPP-SERVER:8080";  // include server port
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), // broadcast address
                    5000 // discovery port
            );
            socket.setBroadcast(true);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

