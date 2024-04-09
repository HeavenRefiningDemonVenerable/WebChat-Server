package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // contains a static List of ChatRoom used to control the existing rooms and their users
    private Map<String, String> usernames = new HashMap<String, String>();
    private static Map<String, String> roomList = new HashMap<String, String>();
    // you may add other attributes as you see fit



    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        roomList.put(session.getId(), roomID);
        System.out.println("Room joined.");
        session.getBasicRemote().sendText("First sample message to the client");
//        accessing the roomID parameter
        System.out.println(roomID);
        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" \\n Welcome, please state your name.\"}");


    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        if(usernames.containsKey(userId)){
            String username = usernames.get(userId);
            String roomId = roomList.get(userId);
            usernames.remove(userId);
            roomList.remove(roomID);
            for(Session peer: session.getOpenSessions()){
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" (Server): " + username + " left the chat.\"}");
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
//        example getting unique userID that sent this message
        String userId = session.getId();
        String roomID = roomList.get(userId);
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("message");

        if (usernames.containsKey(userId)){
            String username = usernames.get(userId);
            System.out.println(username);
            for (Session peer: session.getOpenSessions()){
                if(roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
                }
            }
        }
        else {
            usernames.put(userId, message);
            for (Session peer: session.getOpenSessions()){
                if (!peer.getId().equals(userId)){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" (Server): " + message + " joined the chat room.\"}");
                }
                else {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\" (Server): Welcome, " + message + "\"}");
                }
            }
        }

//        Example conversion of json messages from the client
        //        JSONObject jsonmsg = new JSONObject(comm);
//        String val1 = (String) jsonmsg.get("attribute1");
//        String val2 = (String) jsonmsg.get("attribute2");

        // handle the messages


    }


}