// Elements from the DOM
const roomDisplay = document.getElementById('current-room');
const chatWindow = document.getElementById('chat-window');
const chatInput = document.getElementById('chat-input');
const sendMessageButton = document.getElementById('send-message');
const roomListElement = document.getElementById('room-list');

// WebSocket instance
let ws;

// Username for the chat session
let username;

// Prompt for the username when the page loads
document.addEventListener('DOMContentLoaded', () => {
    username = prompt("Please enter your username") || "Anonymous";
    newRoom(); // Initialize a new room
});

// Function to fetch a new room ID and enter it
function newRoom() {
    // Calling the ChatServlet to retrieve a new room ID
    let callURL = "/WSChatServer-1.0-SNAPSHOT/chat-servlet?action=create";
    fetch(callURL)
        .then(response => response.text())
        .then(roomCode => {
            if (!roomCode.startsWith("Error")) {
                enterRoom(roomCode); // Enter the room with the new code
            } else {
                console.error('Error fetching the new room:', roomCode);
            }
        })
        .catch(error => console.error('Error fetching the new room:', error));
}

// Function to enter a room with a given code
function enterRoom(code) {
    chatWindow.innerHTML = '';
    // Update the UI to show the current room code
    roomDisplay.textContent = `Room: ${code}`;

    // Close any existing WebSocket connection
    if (ws) {
        if (ws.readyState === WebSocket.OPEN) {
            ws.close(1000, "Switching rooms"); // 1000 is the code for normal closure
        }
    }

    // Create the WebSocket connection and attach event listeners
    ws = new WebSocket(`ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/${code}`);
    ws.addEventListener('open', onWsOpen);
    ws.addEventListener('message', onWsMessage);
    ws.addEventListener('close', onWsClose);
    ws.addEventListener('error', onWsError);
}

function updateRoomList(rooms) {
    roomListElement.innerHTML = ''; // Clear the current list
    rooms.forEach(room => {
        const roomElement = document.createElement('div');
        roomElement.textContent = room;
        roomElement.onclick = () => switchRoom(room); // Clicking a room name will switch to that room
        roomListElement.appendChild(roomElement);
    });
}

function switchRoom(roomCode) {
    // Check if already in the requested room to avoid unnecessary switching
    if (roomDisplay.textContent.includes(roomCode)) {
        console.log(`Already in room: ${roomCode}`);
        return;
    }

    // Enter the new room
    enterRoom(roomCode);
}

// WebSocket event handlers
function onWsOpen() {
    console.log(`Connected to room: ${roomDisplay.textContent.replace('Room: ', '')}`);
    // Send the username to the server
    ws.send(JSON.stringify({ type: 'join', username: username }));
}

function onWsMessage(event) {
    const data = JSON.parse(event.data);
    if (data.type === 'roomList') {
        updateRoomList(data.rooms);
    } else if (data.type === 'chat') {
        displayMessage(data);
    }
}

function onWsClose(event) {
    console.log('Disconnected from WebSocket');
}

function onWsError(error) {
    console.error('WebSocket error:', error);
}

// Function to send a chat message
function sendMessage() {
    const message = chatInput.value.trim();
    if (message && ws && ws.readyState === WebSocket.OPEN) {
        // Include the username and timestamp in the message sent to the server
        const now = new Date();
        ws.send(JSON.stringify({
            type: 'chat',
            username: username,
            message: message,
            timestamp: now.toISOString()
        }));
        chatInput.value = ''; // Clear the input after sending
    }
}

// Function to display a message in the chat window
function displayMessage(data) {
    const messageElement = document.createElement('div');
    const timestamp = data.timestamp ? new Date(data.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString();
    messageElement.textContent = `${timestamp} ${data.username}: ${data.message}`;
    chatWindow.appendChild(messageElement);
    chatWindow.scrollTop = chatWindow.scrollHeight; // Scroll to the bottom of the chat window
}

// Event listener for the send message button
sendMessageButton.addEventListener('click', sendMessage);

// Event listener for pressing "Enter" in the input field
chatInput.addEventListener('keypress', function(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
});
