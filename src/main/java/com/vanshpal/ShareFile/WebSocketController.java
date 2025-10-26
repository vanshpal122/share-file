package com.vanshpal.ShareFile;

import com.vanshpal.ShareFile.service.HelperClasses.SenderMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @MessageMapping("/send") //ensures that, if a msg is sent to the /send destination, the senderMsg() method is called
    @SendTo("/topic/receiveFiles")
    public SenderMessage senderMsg(SenderMessage senderMsg) {
        System.out.println(senderMsg.toString());
        return senderMsg;
    }
}