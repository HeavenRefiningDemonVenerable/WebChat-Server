package com.example.webchatserver;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(name = "ChatServlet", urlPatterns = {"/chat-servlet"})
public class ChatServlet extends HttpServlet {

    private static final ConcurrentHashMap<String, String> rooms = new ConcurrentHashMap<>();

    public String generateUniqueRoomID(int length) {
        String generatedString;
        do {
            generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        } while (rooms.containsKey(generatedString));
        rooms.put(generatedString, "");
        return generatedString;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("create".equals(action)) {
            createRoom(response);
        } else if ("list".equals(action)) {
            listRooms(response);
        } else {
            sendErrorResponse(response, "Invalid action.");
        }
    }

    private void createRoom(HttpServletResponse response) throws IOException {
        String roomID = generateUniqueRoomID(5);
        response.setContentType("text/plain");
        response.getWriter().println(roomID);
    }

    private void listRooms(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.println("{\"rooms\": \"" + String.join(", ", rooms.keySet()) + "\"}");
    }

    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = response.getWriter();
        out.println(errorMessage);
    }

    @Override
    public void destroy() {
    }
}
