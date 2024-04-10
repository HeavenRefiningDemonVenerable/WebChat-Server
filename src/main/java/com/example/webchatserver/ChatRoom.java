package com.example.webchatserver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ChatRoom {
    private String code;
    private Map<String, UserDetail> users = new HashMap<>();

    public ChatRoom(String code, String userSessionId){
        this.code = code;
        addUser(userSessionId, ""); // Initialize with an empty username, indicating they've not set it yet
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getUsernames() {
        Map<String, String> usernames = new HashMap<>();
        users.forEach((sessionId, userDetails) -> usernames.put(sessionId, userDetails.username));
        return usernames;
    }

    public void setUserName(String userID, String name) {
        if(users.containsKey(userID)) {
            users.get(userID).username = name;
        } else {
            addUser(userID, name);
        }
    }

    public void removeUser(String userID){
        users.remove(userID);
    }

    public boolean inRoom(String userID){
        return users.containsKey(userID);
    }

    private void addUser(String sessionId, String username) {
        users.put(sessionId, new UserDetail(username, Instant.now()));
    }

    private static class UserDetail {
        String username;
        Instant lastActive;

        UserDetail(String username, Instant lastActive) {
            this.username = username;
            this.lastActive = lastActive;
        }
    }
}
