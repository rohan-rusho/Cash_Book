package com.moneytrackultra.cashbook;

public class User {
    public String uid;
    public String email;
    public String displayName;
    public String photoUrl;          // nullable
    public long createdAtEpoch;

    public User(String uid, String email, String displayName, String photoUrl, long createdAtEpoch) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.createdAtEpoch = createdAtEpoch;
    }
}