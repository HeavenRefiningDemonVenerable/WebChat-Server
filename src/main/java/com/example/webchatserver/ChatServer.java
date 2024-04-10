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

    private static Map<String, String> usernames = new ConcurrentHashMap<>();
    private static Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static Map<String, Set<Session>> roomSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) {
        // Associate session with a username and a room
        sessions.put(session.getId(), session);
        roomSessions.computeIfAbsent(roomID, k -> ConcurrentHashMap.newKeySet()).add(session);
        broadcastRoomList();
        System.out.println("New connection with client: " + session.getId() + " in room: " + roomID);
    }

    @OnClose
    public void close(Session session) {
        String userId = session.getId();
        // Find which room the session is part of and remove it
        roomSessions.forEach((roomID, sessionSet) -> {
            if (sessionSet.remove(session)) {
                // When a user leaves, broadcast it to others in the room
                String username = usernames.getOrDefault(userId, "Anonymous");
                String message = String.format("{\"type\": \"chat\", \"user\": \"Server\", \"message\": \"%s has left the chat.\", \"timestamp\": \"%s\"}", username, getCurrentTimestamp());
                broadcastMessageToRoom(message, roomID);
            }
        });
        usernames.remove(userId);
        sessions.remove(userId);
        broadcastRoomList();
    }

    @OnMessage
    public void handleMessage(String message, Session session, @PathParam("roomID") String roomID) {
        String userId = session.getId();
        JSONObject jsonMsg = new JSONObject(message);
        String type = jsonMsg.getString("type");


        if ("join".equals(type)) {
            String username = jsonMsg.getString("username");
            usernames.put(userId, username);
            String joinMsg = String.format("{\"type\": \"chat\", \"user\": \"Server\", \"message\": \"%s has joined the chat.\", \"timestamp\": \"%s\"}", username, getCurrentTimestamp());
            broadcastMessageToRoom(joinMsg, roomID);
        } else if ("chat".equals(type)) {
            String username = usernames.get(userId);
            String chatMessage = jsonMsg.getString("message");
            String chatMsg = String.format("{\"type\": \"chat\", \"user\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\"}", username, chatMessage, getCurrentTimestamp());
            broadcastMessageToRoom(chatMsg, roomID);
        }
    }



    private void broadcastRoomList() {
        Set<String> roomIds = roomSessions.keySet();
        String roomListMessage = new JSONObject()
                .put("type", "roomList")
                .put("rooms", new JSONArray(roomIds))
                .toString();
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(roomListMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void broadcastMessageToRoom(String message, String roomID) {
        roomSessions.getOrDefault(roomID, Collections.emptySet())
                .forEach(session -> {
                    try {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }
}
