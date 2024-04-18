package com.example.webchatserver;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TimeZone;

@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {
    private static final Map<String, String> usernames = new ConcurrentHashMap<>();
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<String, Set<Session>> roomSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) {
        sessions.put(session.getId(), session);
        roomSessions.computeIfAbsent(roomID, k -> ConcurrentHashMap.newKeySet()).add(session);
        broadcastRoomList();
        System.out.println("New connection with client: " + session.getId() + " in room: " + roomID);
    }

    @OnClose
    public void close(Session session) {
        String userId = session.getId();
        roomSessions.values().forEach(sessionSet -> sessionSet.remove(session));
        usernames.remove(userId);
        sessions.remove(userId);
        broadcastRoomList();
    }

    @OnMessage
    public void handleMessage(String message, Session session, @PathParam("roomID") String roomID) {
        JSONObject jsonMsg = new JSONObject(message);
        String userId = session.getId();
        processMessage(userId, jsonMsg, roomID);
    }

    private void processMessage(String userId, JSONObject jsonMsg, String roomID) {
        String type = jsonMsg.optString("type");
        switch (type) {
            case "join":
                handleJoin(userId, jsonMsg, roomID);
                break;
            case "chat":
                handleChat(userId, jsonMsg, roomID);
                break;
            default:
                System.err.println("Unsupported message type: " + type);
        }
    }

    private void handleJoin(String userId, JSONObject jsonMsg, String roomID) {
        String username = jsonMsg.getString("username");
        usernames.put(userId, username);
        broadcastMessageToRoom(String.format("{\"type\": \"chat\", \"username\": \"Server\", \"message\": \"%s has joined the chat.\", \"timestamp\": \"%s\"}", username, getCurrentTimestamp()), roomID);
    }

    private void handleChat(String userId, JSONObject jsonMsg, String roomID) {
        String username = usernames.getOrDefault(userId, "Anonymous");
        String message = jsonMsg.getString("message");
        broadcastMessageToRoom(String.format("{\"type\": \"chat\", \"username\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\"}", username, message, getCurrentTimestamp()), roomID);
    }

    private void broadcastRoomList() {
        JSONArray roomIds = new JSONArray(roomSessions.keySet());
        String roomListMessage = new JSONObject().put("type", "roomList").put("rooms", roomIds).toString();
        broadcastMessage(roomListMessage);
    }

    private void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }
        });
    }

    private void broadcastMessageToRoom(String message, String roomID) {
        roomSessions.getOrDefault(roomID, Collections.emptySet()).forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message to room: " + e.getMessage());
                }
            }
        });
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }
}
