package io.cliper;


import java.io.Serializable;

/**
 * Created by Zhe on 17-05-2016.
 */
public class ChatMessage implements Serializable {
    private boolean isMe;
    private String message;
    private String dateTime;

    public ChatMessage(boolean a, String b, String c) {
        this.isMe = a;
        this.message = b;
        this.dateTime = c;
    }

    public ChatMessage() {
        super();
    }

    public boolean getIsme() {
        return isMe;
    }

    public void setMe(boolean isMe) {
        this.isMe = isMe;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return dateTime;
    }

    public void setDate(String dateTime) {
        this.dateTime = dateTime;
    }
}