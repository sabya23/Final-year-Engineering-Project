package tools;

import types.MessageInfo;

/**
 * Created by sanchita on 2/5/15.
 */
public class MessageController {

    private static MessageInfo[] messages = null;

    public static void setMessageInfo(MessageInfo[] messageInfo) {
        MessageController.messages = messageInfo;
    }

    public static MessageInfo checkMessage(String username, String message) {
        MessageInfo result = null;
        if (messages != null) {
            for (int i = 0; i < messages.length; i++) {
                if(messages[i].userId.equals(username) && messages[i].messageText.contains(message)) {
                    result = messages[i];
                    break;
                }
            }
        }
        return result;
    }

    public static MessageInfo getMessage(String username) {
        MessageInfo result = null;
        if (messages != null) {
            for (int i = 0; i < messages.length; i++) {
                result = messages[i];
                break;
            }
        }
        return result;
    }

    public static MessageInfo[] getMessages() {
        return messages;
    }
}