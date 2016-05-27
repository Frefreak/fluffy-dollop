package io.cliper;


/**
 * Created by Zhe on 17-05-2016.
 */
public class ChatMessage {
    private boolean isMe;
    private String message;
    private String dateTime;

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