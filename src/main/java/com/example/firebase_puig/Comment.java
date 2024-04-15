package com.example.firebase_puig;

public class Comment {
    public String uid;
    public String author;
    public String content;
    public long timestamp;

    public Comment() {
        // Required empty public constructor for Firestore
    }

    public Comment(String uid, String author, String content, long timestamp) {
        this.uid = uid;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
    }
}
