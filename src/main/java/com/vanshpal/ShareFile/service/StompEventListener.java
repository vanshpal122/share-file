package com.vanshpal.ShareFile.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StompEventListener {
    //Maintaing list of subscribers
    private final Map<String, Set<String>> topicSubscribers = new ConcurrentHashMap<>(); //"/topic/chat" → ["session1"]


    //EventListener are provided by the Spring Messaging that comes with WebSocket library
    //SessionSubscribeEvent → published when a client sends a SUBSCRIBE frame.
    //SessionUnsubscribeEvent → when client sends an UNSUBSCRIBE.
    //SessionDisconnectEvent → when the WebSocket connection closes.

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {    //when a user subscribe to a topic
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        String destination = (String) event.getMessage().getHeaders().get("simpDestination");

        topicSubscribers.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        String sessionId =  (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.remove(sessionId));
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.remove(sessionId));
    }

    public Set<String> getSubscribers(String topic) {
        return topicSubscribers.getOrDefault(topic, Set.of());
    }
}
