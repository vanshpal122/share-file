package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.service.HelperClasses.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StompEventListener {

    private static String receiversDestination = "/topic/receive";
    //Maintaing list of subscribers
    private final Map<String, Set<User>> topicSubscribers = new ConcurrentHashMap<>(); //"/topic/chat" → ["session1"]
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    //EventListener are provided by the Spring Messaging that comes with WebSocket library
    //SessionSubscribeEvent → published when a client sends a SUBSCRIBE frame.
    //SessionUnsubscribeEvent → when client sends an UNSUBSCRIBE.
    //SessionDisconnectEvent → when the WebSocket connection closes.

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {    //when a user subscribe to a topic
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        String destination = (String) event.getMessage().getHeaders().get("simpDestination");
        String userName = (String) event.getMessage().getHeaders().get("userName");

        User user = new User(sessionId, userName);

        topicSubscribers.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet()).add(user);
        broadCastSubscribers(receiversDestination);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.removeIf(rec -> rec.sessionId().equals(sessionId)));
        broadCastSubscribers(receiversDestination);
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.removeIf(rec -> rec.sessionId().equals(sessionId)));
        broadCastSubscribers(receiversDestination);
    }

    private void broadCastSubscribers(String topic) {
        Set<User> subscribers = getSubscribers(topic);
        simpMessagingTemplate.convertAndSend("topic/getReceivers", subscribers);
    }

    public Set<User> getSubscribers(String topic) {
        return topicSubscribers.getOrDefault(topic, Set.of());
    }
}
