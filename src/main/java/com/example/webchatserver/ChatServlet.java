package com.example.webchatserver;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@WebServlet(name = "ChatServlet", urlPatterns = {"/chat-servlet"})
public class ChatServlet extends HttpServlet {
    private static final int ROOM_ID_LENGTH = 5;
    private static final ConcurrentHashMap<String, String> rooms = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(ChatServlet.class.getName());

    public String generateUniqueRoomID() {
        String generatedString;
        do {
            generatedString = RandomStringUtils.randomAlphanumeric(ROOM_ID_LENGTH).toUpperCase();
        } while (rooms.containsKey(generatedString));
        rooms.put(generatedString, "");
        return generatedString;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        try {
            if ("create".equals(action)) {
                createRoom(response);
            } else if ("list".equals(action)) {
                listRooms(response);
            } else {
                sendErrorResponse(response, "Invalid action.");
            }
        } catch (Exception e) {
            logger.severe("Error handling request: " + e.getMessage());
            sendErrorResponse(response, "Internal server error.");
        }
    }

    private void createRoom(HttpServletResponse response) throws IOException {
        String roomID = generateUniqueRoomID();
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("text/plain");
        response.getWriter().println(roomID);
    }

    private void listRooms(HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject();
        json.put("rooms", rooms.keySet());
        response.setContentType("application/json");
        response.getWriter().println(json.toString());
    }

    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.getWriter().println(errorMessage);
    }

    @Override
    public void destroy() {
        logger.info("ChatServlet is being destroyed");
    }
}
