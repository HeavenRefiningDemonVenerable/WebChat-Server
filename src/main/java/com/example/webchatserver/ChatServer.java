package com.example.webchatserver;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map; // Import for Map
import java.util.HashMap; // Import for HashMap

@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    private Map<String, String> usernames = new HashMap<>();
    private static Map<String, String> roomList = new HashMap<>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException {
        roomList.put(session.getId(), roomID);
        System.out.println("Room joined: " + roomID);
        session.getBasicRemote().sendText("Welcome, please state your name.");
    }

    @OnClose
    public void close(Session session) throws IOException {
        String userId = session.getId();
        if (usernames.containsKey(userId)) {
            String username = usernames.get(userId);
            String roomId = roomList.get(userId); // Corrected the variable name to 'roomId' for consistency
            usernames.remove(userId);
            roomList.remove(userId); // Correctly remove the user from the roomList using userId
            for (Session peer : session.getOpenSessions()) {
                if (roomList.get(peer.getId()).equals(roomId)) { // Ensure message is sent only to peers in the same room
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" (Server): " + username + " left the chat.\"}");
                }
            }
        }
    }

    @OnMessage
    public void handleMessage(String message, Session session) throws IOException {
        String userId = session.getId();
        String roomID = roomList.get(userId);
        JSONObject jsonMsg = new JSONObject(message);
        String type = jsonMsg.getString("type");

        if ("name".equals(type)) {
            String name = jsonMsg.getString("message");
            usernames.put(userId, name);
            for (Session peer : session.getOpenSessions()) {
                if (roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" (Server): " + name + " joined the chat room.\"}");
                }
            }
        } else if ("chat".equals(type)) {
            String chatMessage = jsonMsg.getString("message");
            String username = usernames.getOrDefault(userId, "Anon");
            for (Session peer : session.getOpenSessions()) {
                if (roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + chatMessage + "\"}");
                }
            }
        }
    }
}
