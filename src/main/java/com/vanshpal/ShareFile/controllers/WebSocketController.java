package com.vanshpal.ShareFile.controllers;

import com.vanshpal.ShareFile.service.sessionService.helperClasses.NegotiationData;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.SessionWithTokenContainer;
import com.vanshpal.ShareFile.service.entityClasses.User;
import com.vanshpal.ShareFile.service.sessionService.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionService sessionService;

    // Receiver presence broadcast (public)
    @MessageMapping("/broadcast")
    @SendTo("/topic/getReceivers")
    public User senderReceiverStatus(User user) {
        return user;
    }

    // Sender → Receiver (metadata)
    @MessageMapping("/sendTo/{userId}")
    public void senderMetadataFromSender(
            @DestinationVariable String userId,
            NegotiationData metadata) {

        messagingTemplate.convertAndSendToUser(
                userId,                 // receiver identity
                "/queue/negotiation",   // private queue
                metadata
        );
    }

    // Receiver → Sender (ack)
    @MessageMapping("/sendAck/{senderId}")
    public void senderMetadataFromReceiver(
            @DestinationVariable String senderId,
            NegotiationData ack) {

        if(ack.listOfFiles().isEmpty()) {
            return;
        }

        SessionWithTokenContainer sessionWithTokenContainer = sessionService.createSession(senderId, ack);


        messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/session",
                sessionWithTokenContainer.senderSession()
        );

        messagingTemplate.convertAndSendToUser(
                ack.user().userId(),
                "/queue/session",
                sessionWithTokenContainer.receiverSession()
        );
    }
}