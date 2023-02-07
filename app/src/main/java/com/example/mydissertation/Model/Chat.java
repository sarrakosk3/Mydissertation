package com.example.mydissertation.Model;

import com.google.firebase.firestore.Blob;

public class Chat {

    private String sender;
    private String receiver;
    private Blob message;
    private Blob iv;

    public Chat(String sender, String receiver, Blob message, Blob iv) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.iv = iv;
    }

    public Chat() {

    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Blob getMessage() {
        return message;
    }

    public void setMessage(Blob message) {
        this.message = message;
    }

    public Blob getIv() { return iv; }

    public void setIv(Blob iv) { this.iv = iv; }
}
