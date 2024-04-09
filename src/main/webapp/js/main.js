// Elements from the DOM
const roomDisplay = document.getElementById('current-room');
const chatWindow = document.getElementById('chat-window');
const chatInput = document.getElementById('chat-input');
const sendMessageButton = document.getElementById('send-message');

// WebSocket instance
let ws;

// Function to fetch a new room ID and enter it
function newRoom() {
    // Calling the ChatServlet to retrieve a new room ID
    let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL)
        .then(response => response.text())
        .then(roomCode => enterRoom(roomCode)) // Enter the room with the new code
        .catch(error => console.error('Error fetching the new room:', error));
}

// Function to enter a room with a given code
function enterRoom(code) {
    // Update the UI to show the current room code
    roomDisplay.textContent = `Room: ${code}`;

    // Close any existing WebSocket connection
    if (ws) {
        ws.close();
    }

    // Create the WebSocket connection
    ws = new WebSocket(`ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/${code}`);

    // Set up WebSocket event listeners
    ws.addEventListener('open', () => console.log(`Connected to room: ${code}`));
    ws.addEventListener('message', event => displayMessage(JSON.parse(event.data)));
    ws.addEventListener('close', event => console.log('Disconnected from WebSocket'));
    ws.addEventListener('error', error => console.error('WebSocket error:', error));
}

// Function to send a chat message
function sendMessage() {
    const message = chatInput.value.trim();
    if (message && ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ user: "Anonymous", message: message }));
        chatInput.value = ''; // Clear the input after sending
    }
}

// Function to display a message in the chat window
function displayMessage(data) {
    const messageElement = document.createElement('div');
    messageElement.textContent = `${data.user}: ${data.message}`;
    chatWindow.appendChild(messageElement);

    // Scroll to the bottom of the chat window
    chatWindow.scrollTop = chatWindow.scrollHeight;
}

// Initialize a new room when the script loads
newRoom();

// Event listener for the send message button
sendMessageButton.addEventListener('click', sendMessage);

// Event listener for pressing "Enter" in the input field
chatInput.addEventListener('keypress', function(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
});
