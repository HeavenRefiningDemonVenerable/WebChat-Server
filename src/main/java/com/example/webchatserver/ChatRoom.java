package com.example.webchatserver;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    private final String code;
    private final Map<String, UserDetail> users = new ConcurrentHashMap<>();

    public ChatRoom(String code, String userSessionId) {
        this.code = code;
        addUser(userSessionId, ""); // Initialize with an empty username
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getUsernames() {
        Map<String, String> usernames = new ConcurrentHashMap<>();
        users.forEach((sessionId, userDetails) -> usernames.put(sessionId, userDetails.getUsername()));
        return usernames;
    }

    public void setUserName(String userID, String name) {
        users.computeIfPresent(userID, (id, userDetails) -> {
            userDetails.setUsername(name);
            return userDetails;
        });
    }

    public void removeUser(String userID) {
        users.remove(userID);
    }

    public boolean inRoom(String userID) {
        return users.containsKey(userID);
    }

    private void addUser(String sessionId, String username) {
        users.put(sessionId, new UserDetail(username));
    }

    private static class UserDetail {
        private String username;
        private Instant lastActive;

        UserDetail(String username) {
            setUsername(username);
            updateLastActive();
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
            updateLastActive();
        }

        public Instant getLastActive() {
            return lastActive;
        }

        public void updateLastActive() {
            this.lastActive = Instant.now();
        }
    }
}
