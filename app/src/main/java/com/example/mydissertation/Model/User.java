package com.example.mydissertation.Model;


import com.google.firebase.firestore.Blob;

public class User {     //κλάση χρήστη

    private String id;
    private String username;
    private String ImageURL;
    private String status;          //4-7-2021: κατασταση χρήστη (online/offline)
    private Blob publicKey;

    public User(String id, String username, String imageUrl, String status, Blob publicKey) {
        this.id = id;
        this.username = username;
        this.ImageURL = imageUrl;
        this.status = status;
        this.publicKey = publicKey;
    }

    public User() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImageUrl() {
        return ImageURL;
    }

    public void setImageUrl(String imageUrl) {
        this.ImageURL = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public com.google.firebase.firestore.Blob getPublicKey() { return publicKey; }

    public void setPublicKey(Blob publicKey) { this.publicKey = publicKey; }
}
