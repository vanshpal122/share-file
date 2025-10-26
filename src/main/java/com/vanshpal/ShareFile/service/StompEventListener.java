package com.vanshpal.ShareFile.service;

import com.vanshpal.ShareFile.service.HelperClasses.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
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

    private static final String RECEIVERS_DEST = "/topic/receive";
    private final Map<String, Set<User>> topicSubscribers = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        String userName = accessor.getFirstNativeHeader("userName");
        if (userName == null) {
            userName = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
        }

        User user = new User(sessionId, userName);
        topicSubscribers.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet()).add(user);

        broadCastSubscribers(destination);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.removeIf(rec -> rec.sessionId().equals(sessionId)));
        topicSubscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        broadCastSubscribers(RECEIVERS_DEST);
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        topicSubscribers.values().forEach(set -> set.removeIf(rec -> rec.sessionId().equals(sessionId)));
        topicSubscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        broadCastSubscribers(RECEIVERS_DEST);
    }

    private void broadCastSubscribers(String topic) {
        Set<User> subscribers = getSubscribers(topic);
        simpMessagingTemplate.convertAndSend("/topic/getReceivers", subscribers);
    }

    public Set<User> getSubscribers(String topic) {
        return topicSubscribers.getOrDefault(topic, Set.of());
    }
}
